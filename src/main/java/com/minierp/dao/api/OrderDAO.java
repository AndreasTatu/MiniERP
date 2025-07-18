package com.minierp.dao.api;

import com.minierp.common.exceptions.OrderNotFoundException;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;

import java.sql.SQLException;
import java.util.List;

public interface OrderDAO {

    //CRUD-Methods

    //create
    void createOrder(Order order, List<OrderItem> orderItems) throws SQLException;

    //read
    Order findOrderByID(int orderID) throws OrderNotFoundException, SQLException;
    List<Order> findAllOrders() throws SQLException;
    List<Order> findAllOrdersByStatus(OrderStatus orderStatus) throws SQLException;
    List<Order> findAllOrdersByCustomer(int customerID) throws SQLException;
    List<OrderItem> findAllOrderItemsByOrder(int orderID) throws  SQLException;

    //update
    void updateOrderStatus(int orderID, OrderStatus newOrderStatus) throws OrderNotFoundException, SQLException;


}
