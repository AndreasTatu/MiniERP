package com.minierp.service.order;

import com.minierp.dao.order.OrderDAO;
import com.minierp.dao.product.ProductDAO;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;
import com.minierp.model.order.OrderAllocation;
import com.minierp.model.order.StockSelection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final OrderInventoryWorkflow inventory;

    public OrderServiceImpl(OrderDAO orderDAO, ProductDAO productDAO, OrderInventoryWorkflow inventory) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.inventory = inventory;
    }

    @Override
    public void createOrder(Order order, List<OrderItem> orderItems){

        if (order == null)
            throw new IllegalArgumentException("Order-object must not be null");

        if (orderItems == null || orderItems.isEmpty())
            throw new IllegalArgumentException("Order must contain at least one OrderItem");

        if (order.getCustomerID() <= 0)
            throw new IllegalArgumentException("customerID must be positive.");

        if (order.getOrderDateTime() == null)
            throw new IllegalArgumentException("Order date must not be null.");
        if (order.getOrderStatus() != OrderStatus.PENDING)
            throw new IllegalArgumentException("New orders must have PENDING status.");
        BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
        BigDecimal tax = order.getTax() == null ? BigDecimal.ZERO : order.getTax();
        if (discount.signum() < 0 || discount.compareTo(BigDecimal.ONE) > 0 || tax.signum() < 0)
            throw new IllegalArgumentException("Discount must be between 0 and 1; tax must not be negative.");

        // Validate and compute subtotal from orderItems (cheap checks first, DB checks later)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {

            if (item == null)
                throw new IllegalArgumentException("OrderItem must not be null.");

            if (item.getProductID() <= 0)
                throw new IllegalArgumentException("OrderItem.productID must be positive.");

            if (item.getQuantity() <= 0)
                throw new IllegalArgumentException("OrderItem.quantity must be greater than zero for productID=" + item.getProductID());

            if (item.getUnitPrice() == null)
                throw new IllegalArgumentException("OrderItem.unitPrice must not be null for productID=" + item.getProductID());

            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("OrderItem.unitPrice must not be negative for productID=" + item.getProductID());


            // accumulate price = unitPrice * quantity
            BigDecimal line = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(line);
        }

        // Validate products in DB (expensive checks)
        for (OrderItem item : orderItems){
            try {
                // productDAO.findProductByID throws ProductNotFoundException / SQLException
                var product = productDAO.findProductByID(item.getProductID());
                if (!product.isActive())
                    throw new IllegalArgumentException("Product with ID " + item.getProductID() + " is not active.");
            }
            catch (SQLException e) {
                throw new RuntimeException("Database access error while validating product with ID " + item.getProductID(), e);
            }

        }

        // Rates are fractions: 0.10 = 10% discount, 0.19 = 19% tax.
        // Round only the final amount, after discount and tax have both been applied.
        order.setDiscount(discount);
        order.setTax(tax);
        order.setTotalPrice(subtotal.multiply(BigDecimal.ONE.subtract(discount))
                .multiply(BigDecimal.ONE.add(tax)).setScale(2, RoundingMode.HALF_UP));
        database(() -> { orderDAO.createOrder(order, orderItems); return null; });
    }
    @Override
    public Order findOrderByID(int orderID) {
        requirePositiveID(orderID);
        return database(() -> orderDAO.findOrderByID(orderID));
    }

    @Override
    public List<Order> findAllOrders() {
        return database(orderDAO::findAllOrders);
    }

    @Override
    public List<Order> findAllOrdersByStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("Order status must not be null.");
        return database(() -> orderDAO.findAllOrdersByStatus(status));
    }

    @Override
    public List<Order> findAllOrdersByCustomer(int customerID) {
        requirePositiveID(customerID);
        return database(() -> orderDAO.findAllOrdersByCustomer(customerID));
    }

    @Override
    public List<OrderItem> findAllOrderItemsByOrder(int orderID) {
        requirePositiveID(orderID);
        return database(() -> orderDAO.findAllOrderItemsByOrder(orderID));
    }

    @Override
    public void updateOrderStatus(int orderID, OrderStatus status, List<StockSelection> selection) {
        requirePositiveID(orderID);
        database(() -> { inventory.transition(orderID, status, selection); return null; });
    }

    @Override
    public List<OrderAllocation> findAllocationsByOrder(int orderID) {
        requirePositiveID(orderID);
        return database(() -> inventory.findAllocations(orderID));
    }

    private void requirePositiveID(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
    }

    private <T> T database(DatabaseCall<T> call) {
        try { return call.run(); }
        catch (SQLException exception) { throw new RuntimeException("Database access error", exception); }
    }

    @FunctionalInterface
    private interface DatabaseCall<T> { T run() throws SQLException; }
}
