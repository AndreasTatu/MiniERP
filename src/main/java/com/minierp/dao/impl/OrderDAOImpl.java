package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.OrderNotFoundException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.dao.api.OrderDAO;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAOImpl implements OrderDAO {

    //CRUD-Methods

    //create
    @Override
    public void createOrder(Order order, List<OrderItem> orderItems) throws SQLException {

        // Define SQL for inserting a new order
        final String insertOrderSQL = "INSERT INTO orders (customerID, orderDateTime, discount, tax, totalPrice, orderStatus) VALUES(?, ?, ?, ?, ?, ?)";

        // Define SQL for inserting order items that belong to the order
        final String insertOrderItemSQL = "INSERT INTO orderItems (orderID, productID, quantity, unitPrice) VALUES (?, ?, ?, ?)";

        // Open a connection to the database
        try(Connection conn = DatabaseConnection.getConnection()) {

            // Turn off auto-commit so we can manage transactions manually
            // This allows us to commit everything at once, or roll back in case of an error
            conn.setAutoCommit(false);

            // Create two PreparedStatements:
            // - one to insert the order
            // - one to insert each orderItem
            try(PreparedStatement insertOrderStmt = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement insertOrderItemStmt = conn.prepareStatement(insertOrderItemSQL)) {

                // Set values for the order INSERT statement
                insertOrderStmt.setInt(1, order.getCustomerID());
                insertOrderStmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDateTime()));
                insertOrderStmt.setBigDecimal(3, order.getDiscount());
                insertOrderStmt.setBigDecimal(4, order.getTax());
                insertOrderStmt.setBigDecimal(5, order.getTotalPrice());
                insertOrderStmt.setString(6, order.getOrderStatus().name());

                // Execute the INSERT and check if it actually inserted a row
                int affectedRowsOrders = insertOrderStmt.executeUpdate();
                if(affectedRowsOrders == 0){
                    conn.rollback(); // Undo everything if nothing was inserted
                    throw new SQLException("Creating order failed: no rows affected.");
                }

                // Get the auto-generated orderID from the database
                try(ResultSet generatedKeys = insertOrderStmt.getGeneratedKeys()){
                    if(!generatedKeys.next()){
                        conn.rollback(); // undo everything if no orderID was generated
                        throw new SQLException("Creating order failed: no orderID obtained.");
                    }

                    // Extract the generated orderID (PK)
                    int orderID = generatedKeys.getInt(1);

/*********************************************************************
//version with .addBatch()-->.executeBatch()-->performance improvement because everything gets handed to the DB at once

                    // inserting all orderItems from the provided list
                for (OrderItem item : orderItems) {
                    itemStmt.setInt(1, orderID);
                    itemStmt.setInt(2, item.getProductID());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setBigDecimal(4, item.getUnitPrice());
                    itemStmt.addBatch();
                }

                int[] itemResults = itemStmt.executeBatch();
                if (itemResults.length != orderItems.size()) {
                    conn.rollback();
                    throw new SQLException("Inserting order items failed, not all items were inserted.");
                }
            }

            conn.commit(); // commiting the DB transaction
        } catch (SQLException ex) {
            conn.rollback(); // undo all in case of error
            throw ex;
        } finally {
            conn.setAutoCommit(true); // reactivating
        }
    }
}
**********************************************************************/


                    // For-each item in the order (List<OrderItem> orderItems)
                    for(OrderItem item : orderItems) {
                        // Set the values for this item in the orderItems table
                        insertOrderItemStmt.setInt(1, orderID); // Link to the created order
                        insertOrderItemStmt.setInt(2, item.getProductID());
                        insertOrderItemStmt.setInt(3, item.getQuantity());
                        insertOrderItemStmt.setBigDecimal(4, item.getUnitPrice());

                        // Execute insert for this item and check if it actually inserted a row
                        int affectedRowsOrderItems = insertOrderItemStmt.executeUpdate();
                        if(affectedRowsOrderItems == 0){
                            conn.rollback(); // If even one item fails, undo everything
                            throw new SQLException("Updating orderItems failed: no rows affected.");
                        }
                    }
                }
                // If we get here, everything was successful: commit the transaction
                conn.commit();

                // Turn auto-commit back on (optional but good practice)
                conn.setAutoCommit(true);
            }
        }
    }



    //read

    private final Order mapResultSetToOrder(ResultSet rs) throws SQLException{
        Timestamp timestamp = rs.getTimestamp("orderDateTime");
        LocalDateTime orderDateTime = timestamp.toLocalDateTime();
        OrderStatus orderStatus = OrderStatus.valueOf(rs.getString("orderStatus"));

        return new Order(
                rs.getInt("orderID"),
                rs.getInt("customerID"),
                orderDateTime,
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("tax"),
                rs.getBigDecimal("totalPrice"),
                orderStatus);
    }

    @Override
    public Order findOrderByID(int orderID) throws OrderNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM orders WHERE orderID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setInt(1, orderID);
            try(ResultSet rs = findStmt.executeQuery()){
                if(!rs.next()){
                    throw new OrderNotFoundException("Order with ID: " + orderID + " not found.");
                }
                return mapResultSetToOrder(rs);
            }
        }
    }


    @Override
    public List<Order> findAllOrders() throws SQLException{

        final String findSQL = "SELECT * FROM orders";
        final List<Order> orderList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            try(ResultSet rs = findStmt.executeQuery()){
                while(rs.next()){
                    orderList.add(mapResultSetToOrder(rs));
                }
                return orderList;
            }
        }
    }


    @Override
    public List<Order> findAllOrdersByStatus(OrderStatus orderStatus) throws SQLException{

        final String findSQL = "SELECT * FROM orders WHERE orderStatus = ?";
        final List<Order> orderList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setString(1, orderStatus.name());
            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    orderList.add(mapResultSetToOrder(rs));
                }
                return orderList;
            }
        }
    }


    @Override
    public List<Order> findAllOrdersByCustomer(int customerID) throws SQLException{

        final String findSQL = "SELECT * FROM orders WHERE customerID = ?";
        final List<Order> orderList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setInt(1, customerID);
            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    orderList.add(mapResultSetToOrder(rs));
                }
                return orderList;
            }
        }

    }


    @Override
    public List<OrderItem> findAllOrderItemsByOrder(int orderID) throws  SQLException{

        final String findSQL = "SELECT * FROM orderItems WHERE orderID = ?";
        final List<OrderItem> orderItemList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setInt(1, orderID);
            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    orderItemList.add(new OrderItem(rs.getInt("orderItemID"), rs.getInt("orderID"), rs.getInt("productID"), rs.getInt("quantity"), rs.getBigDecimal("unitPrice")));
                }
                return orderItemList;
            }
        }

    }


    //update
    @Override
    public void updateOrderStatus(int orderID, OrderStatus newOrderStatus) throws OrderNotFoundException, SQLException {

        final String checkSQL = "SELECT 1 FROM orders WHERE orderID = ?";
        final String updateSQL = "UPDATE orders SET orderStatus = ? WHERE orderID = ? ";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL)){

            checkStmt.setInt(1, orderID);
            try(ResultSet rs = checkStmt.executeQuery()){
                if (!rs.next()){
                    throw new OrderNotFoundException("Update failed: order with ID: " + orderID + " not found.");
                }
            }
            updateStmt.setString(1, newOrderStatus.name());
            updateStmt.setInt(2, orderID);
            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Update failed: no Rows affected.");
            }
        }
    }


}
