package com.minierp.service.order;

import com.minierp.common.database.JdbcTransactions;
import com.minierp.common.database.SchemaInitializer;
import com.minierp.common.exceptions.StockConflictException;
import com.minierp.dao.customer.CustomerDAOImpl;
import com.minierp.dao.order.*;
import com.minierp.dao.product.ProductDAOImpl;
import com.minierp.dao.stock.StockDAOImpl;
import com.minierp.model.customer.Customer;
import com.minierp.model.order.*;
import com.minierp.model.product.Product;
import com.minierp.model.stock.Stock;
import com.minierp.service.stock.StockServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderInventoryIntegrationTest {
    Connection connection;
    OrderDAOImpl orderDAO;
    StockDAOImpl stockDAO;
    OrderServiceImpl orders;
    StockServiceImpl stocks;
    int customerID, productID;
    Stock first, second;

    @BeforeEach
    void setup() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        SchemaInitializer.initialize(connection);
        var customers = new CustomerDAOImpl(connection);
        customers.createCustomer(new Customer("Test", "Berlin", null, "inventory@example.test", "123"));
        customerID = customers.findCustomerByEmail("inventory@example.test").getCustomerID();
        var products = new ProductDAOImpl(connection);
        products.createProduct(new Product("Artikel", null, new BigDecimal("250"), BigDecimal.TEN, 1));
        productID = products.findProductByName("Artikel").getProductID();
        stockDAO = new StockDAOImpl(connection);
        var transactions = new JdbcTransactions(connection);
        stocks = new StockServiceImpl(stockDAO, products, transactions);
        first = new Stock(productID, "A-01-01", 8, 100, 2, true);
        second = new Stock(productID, "B-01-01", 6, 100, 1, true);
        stocks.createStock(first); stocks.createStock(second);
        orderDAO = new OrderDAOImpl(connection);
        orders = new OrderServiceImpl(orderDAO, products, new OrderInventoryWorkflow(orderDAO, products, stockDAO,
                new OrderAllocationDAOImpl(connection), transactions));
    }

    @AfterEach
    void close() throws Exception { connection.close(); }

    @Test
    void manualSelectionReservesAndShipsExactlyOnceAndPreservesManualReservations() {
        var order = order(2, 2); // Duplicate product lines must be combined for allocation validation.
        var selected = List.of(new StockSelection(first.getStockID(), 1), new StockSelection(second.getStockID(), 3));
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED, selected);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED, selected);
        assertStock(first, 8, 3); assertStock(second, 6, 4);
        assertEquals(2, orders.findAllocationsByOrder(order.getOrderID()).size());
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.SHIPPED);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.SHIPPED);
        assertStock(first, 7, 2); assertStock(second, 3, 1);
        assertTrue(orders.findAllocationsByOrder(order.getOrderID()).stream().allMatch(row -> row.state() == OrderAllocation.State.SHIPPED));
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.DELIVERED);
        assertStock(first, 7, 2);
        assertThrows(IllegalArgumentException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.CANCELLED));
    }

    @Test
    void cancellationReleasesOnlyThisOrdersReservations() {
        var a = order(3); var b = order(2);
        orders.updateOrderStatus(a.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(first.getStockID(), 3)));
        orders.updateOrderStatus(b.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(first.getStockID(), 2)));
        orders.updateOrderStatus(a.getOrderID(), OrderStatus.CANCELLED);
        orders.updateOrderStatus(a.getOrderID(), OrderStatus.CANCELLED);
        assertStock(first, 8, 4); // manual 2 + order b 2
        assertEquals(OrderAllocation.State.RELEASED, orders.findAllocationsByOrder(a.getOrderID()).get(0).state());
        assertEquals(OrderAllocation.State.RESERVED, orders.findAllocationsByOrder(b.getOrderID()).get(0).state());
    }

    @Test
    void competingOrdersCannotOverbookAndConfirmedSelectionCannotBeReplaced() {
        var a = order(4); var b = order(3);
        orders.updateOrderStatus(a.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(first.getStockID(), 4)));
        assertThrows(StockConflictException.class, () -> orders.updateOrderStatus(b.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 3))));
        assertThrows(StockConflictException.class, () -> orders.updateOrderStatus(a.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(second.getStockID(), 4))));
        assertStock(first, 8, 6); assertStock(second, 6, 1);
        assertEquals(OrderStatus.PENDING, orders.findOrderByID(b.getOrderID()).getOrderStatus());
        assertTrue(orders.findAllocationsByOrder(b.getOrderID()).isEmpty());
    }

    @Test
    void shortagesAndIncorrectSelectionsDoNotPartiallyConfirm() {
        var order = order(7);
        assertThrows(StockConflictException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 7))));
        assertThrows(IllegalArgumentException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 2), new StockSelection(second.getStockID(), 2))));
        assertThrows(IllegalArgumentException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 2), new StockSelection(first.getStockID(), 5))));
        assertStock(first, 8, 2); assertStock(second, 6, 1);
        assertEquals(OrderStatus.PENDING, orders.findOrderByID(order.getOrderID()).getOrderStatus());
        assertTrue(orders.findAllocationsByOrder(order.getOrderID()).isEmpty());
    }

    @Test
    void databaseFailureAfterReservationsRollsBackStatusStockAndLedger() throws Exception {
        var order = order(4);
        execute("CREATE TRIGGER reject_confirm BEFORE UPDATE OF orderStatus ON orders WHEN NEW.orderStatus = 'CONFIRMED' BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        assertThrows(RuntimeException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 2), new StockSelection(second.getStockID(), 2))));
        assertStock(first, 8, 2); assertStock(second, 6, 1);
        assertEquals(OrderStatus.PENDING, orders.findOrderByID(order.getOrderID()).getOrderStatus());
        assertTrue(orders.findAllocationsByOrder(order.getOrderID()).isEmpty());
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void shippingFailureAtSecondLocationRollsBackTheFirstLocation() throws Exception {
        var order = order(4);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED,
                List.of(new StockSelection(first.getStockID(), 2), new StockSelection(second.getStockID(), 2)));
        execute("CREATE TRIGGER reject_ship BEFORE UPDATE OF quantity ON stocks WHEN OLD.stockID = " + second.getStockID() + " AND NEW.quantity < OLD.quantity BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        assertThrows(RuntimeException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.SHIPPED));
        assertStock(first, 8, 4); assertStock(second, 6, 3);
        assertEquals(OrderStatus.CONFIRMED, orders.findOrderByID(order.getOrderID()).getOrderStatus());
        assertTrue(orders.findAllocationsByOrder(order.getOrderID()).stream().allMatch(row -> row.state() == OrderAllocation.State.RESERVED));
    }

    @Test
    void stockEditsCannotRemoveOrderReservationsOrMoveTheirLocation() {
        var order = order(3);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(first.getStockID(), 3)));
        assertThrows(StockConflictException.class, () -> stocks.updateStockQuantityAndReserved(first.getStockID(), 8, 2));
        var changed = stocks.findStockByStockID(first.getStockID()); changed.setLocation("C-01-01");
        assertThrows(StockConflictException.class, () -> stocks.updateFullStock(changed));
        assertThrows(StockConflictException.class, () -> stocks.deactivateStock(first.getStockID()));
        stocks.updateStockQuantityAndReserved(first.getStockID(), 10, 4); // manual reservation may change, managed 3 remains protected.
        assertStock(first, 10, 4);
    }

    @Test
    void legacyConfirmedOrdersNeedExplicitAllocationBeforeShipping() throws Exception {
        var order = order(2); orderDAO.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED);
        assertThrows(StockConflictException.class, () -> orders.updateOrderStatus(order.getOrderID(), OrderStatus.SHIPPED));
        assertStock(first, 8, 2);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(second.getStockID(), 2)));
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.SHIPPED);
        assertStock(second, 4, 1);
    }

    @Test
    void workflowPreservesAnEnclosingTransaction() throws Exception {
        var order = order(2);
        connection.setAutoCommit(false);
        orders.updateOrderStatus(order.getOrderID(), OrderStatus.CONFIRMED, List.of(new StockSelection(first.getStockID(), 2)));
        assertFalse(connection.getAutoCommit());
        assertStock(first, 8, 4);
        connection.rollback(); connection.setAutoCommit(true);
        assertStock(first, 8, 2);
        assertTrue(orders.findAllocationsByOrder(order.getOrderID()).isEmpty());
        assertEquals(OrderStatus.PENDING, orders.findOrderByID(order.getOrderID()).getOrderStatus());
    }

    private Order order(int... quantities) {
        var order = new Order(customerID, LocalDateTime.of(2026, 10, 26, 10, 0), BigDecimal.ZERO, BigDecimal.ZERO, null);
        orders.createOrder(order, java.util.Arrays.stream(quantities).mapToObj(qty -> new OrderItem(productID, qty, BigDecimal.TEN)).toList());
        return order;
    }
    private void assertStock(Stock stock, int quantity, int reserved) {
        var actual = stocks.findStockByStockID(stock.getStockID());
        assertEquals(quantity, actual.getQuantity()); assertEquals(reserved, actual.getReserved());
    }
    private void execute(String sql) throws SQLException { try (var statement = connection.createStatement()) { statement.execute(sql); } }
}
