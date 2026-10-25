package com.minierp.dao.order;

import com.minierp.common.exceptions.OrderNotFoundException;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAOImpl implements OrderDAO {

    private final Connection connection;

    public OrderDAOImpl(Connection connection) {
        this.connection = connection;
    }

    //CRUD-Methods

    //create
    @Override
    public void createOrder(Order order, List<OrderItem> orderItems) throws SQLException {
        String orderSql = "INSERT INTO orders (customerID, orderDateTime, discount, tax, totalPrice, orderStatus) VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO orderItems (orderID, productID, quantity, unitPrice) VALUES (?, ?, ?, ?)";
        boolean ownsTransaction = connection.getAutoCommit();
        Savepoint savepoint = null;
        if (ownsTransaction) connection.setAutoCommit(false);
        else savepoint = connection.setSavepoint();
        try (PreparedStatement orderStatement = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
            orderStatement.setInt(1, order.getCustomerID());
            orderStatement.setTimestamp(2, Timestamp.valueOf(order.getOrderDateTime()));
            orderStatement.setBigDecimal(3, order.getDiscount());
            orderStatement.setBigDecimal(4, order.getTax());
            orderStatement.setBigDecimal(5, order.getTotalPrice());
            orderStatement.setString(6, order.getOrderStatus().name());
            if (orderStatement.executeUpdate() != 1) throw new SQLException("Order was not inserted.");
            int orderID;
            try (ResultSet keys = orderStatement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated order ID returned.");
                orderID = keys.getInt(1);
            }
            for (OrderItem item : orderItems) {
                itemStatement.setInt(1, orderID);
                itemStatement.setInt(2, item.getProductID());
                itemStatement.setInt(3, item.getQuantity());
                itemStatement.setBigDecimal(4, item.getUnitPrice());
                if (itemStatement.executeUpdate() != 1) throw new SQLException("Order item was not inserted.");
            }
            if (ownsTransaction) connection.commit();
            else connection.releaseSavepoint(savepoint);
            order.setOrderID(orderID);
        } catch (SQLException | RuntimeException exception) {
            try {
                if (ownsTransaction) connection.rollback();
                else connection.rollback(savepoint);
            } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
            throw exception;
        } finally {
            if (ownsTransaction) connection.setAutoCommit(true);
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

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

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
