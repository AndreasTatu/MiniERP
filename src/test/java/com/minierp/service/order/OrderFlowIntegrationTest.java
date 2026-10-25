package com.minierp.service.order;

import com.minierp.common.database.SchemaInitializer;
import com.minierp.dao.customer.CustomerDAOImpl;
import com.minierp.dao.order.OrderDAOImpl;
import com.minierp.dao.product.ProductDAOImpl;
import com.minierp.model.customer.Customer;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;
import com.minierp.model.product.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderFlowIntegrationTest {
    private Connection connection;
    private OrderDAOImpl dao;
    private OrderServiceImpl service;
    private int customerID;
    private int productID;

    @BeforeEach
    void start() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        SchemaInitializer.initialize(connection);
        var customers = new CustomerDAOImpl(connection);
        customers.createCustomer(new Customer("Testkunde", "Berlin", null, "order@example.test", "123"));
        customerID = customers.findCustomerByEmail("order@example.test").getCustomerID();
        var products = new ProductDAOImpl(connection);
        products.createProduct(new Product("Artikel", null, BigDecimal.ONE, new BigDecimal("33.33"), 0));
        productID = products.findProductByName("Artikel").getProductID();
        dao = new OrderDAOImpl(connection);
        service = new OrderServiceImpl(dao, products);
    }

    @AfterEach
    void stop() throws Exception { if (connection != null) connection.close(); }

    @Test
    void fractionalDiscountAndTaxAreCalculatedThenPersistedWithItems() {
        var order = order();
        order.setTotalPrice(new BigDecimal("9999")); // A supplied total must not override the calculation.
        service.createOrder(order, List.of(new OrderItem(0, productID, 3, new BigDecimal("33.33"))));
        var saved = service.findAllOrders().get(0);
        assertEquals(0, new BigDecimal("107.09").compareTo(saved.getTotalPrice()));
        assertEquals(1, service.findAllOrderItemsByOrder(saved.getOrderID()).size());
        service.updateOrderStatus(saved.getOrderID(), OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, service.findOrderByID(saved.getOrderID()).getOrderStatus());
        assertThrows(IllegalArgumentException.class, () -> service.updateOrderStatus(saved.getOrderID(), OrderStatus.DELIVERED));
    }

    @Test
    void failureInAnItemRollsBackTheWholeOrderAndRestoresTheConnection() throws Exception {
        var order = order();
        order.setTotalPrice(BigDecimal.TEN);
        assertThrows(SQLException.class, () -> dao.createOrder(order, List.of(
                new OrderItem(0, productID, 1, BigDecimal.ONE),
                new OrderItem(0, Integer.MAX_VALUE, 1, BigDecimal.ONE))));
        assertTrue(connection.getAutoCommit());
        assertTrue(dao.findAllOrders().isEmpty());
        try (var statement = connection.createStatement(); var results = statement.executeQuery("SELECT COUNT(*) FROM orderItems")) {
            assertTrue(results.next());
            assertEquals(0, results.getInt(1));
        }
    }

    @Test
    void invalidDiscountDoesNotPersistAnOrder() {
        var order = order();
        order.setDiscount(new BigDecimal("1.01"));
        assertThrows(IllegalArgumentException.class, () -> service.createOrder(order,
                List.of(new OrderItem(0, productID, 1, BigDecimal.ONE))));
        assertTrue(service.findAllOrders().isEmpty());
    }

    private Order order() {
        return new Order(customerID, LocalDateTime.of(2026, 10, 25, 10, 0),
                new BigDecimal("0.10"), new BigDecimal("0.19"), null);
    }
}
