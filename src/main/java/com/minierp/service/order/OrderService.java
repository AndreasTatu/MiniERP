package com.minierp.service.order;

import com.minierp.common.exceptions.OrderNotFoundException;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;

import java.util.List;

/**
 * Service interface for managing orders and related operations.
 *
 * The OrderService layer handles:
 *
 *     Validation of input parameters
 *     Business rules (e.g., allowed status transitions)
 *     Delegation to the DAO layer for persistence
 *     Error handling and exception translation
 *
 *
 * Implementations are expected to be stateless and thread-safe if used in multi-threaded contexts.
 */
public interface OrderService {

    /**
     * Creates a new order along with its associated order items.
     * Performs:
     *      Validation of order data (e.g., customer ID, non-empty order items, positive quantities/prices)
     *      Calculation of total price if not provided
     *
     * @param order      the order to be created
     * @param orderItems the list of order items belonging to the order
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if a database error occurs
     */
    void createOrder(Order order, List<OrderItem> orderItems);

    /**
     * Retrieves an order by its unique ID.
     *
     * @param orderID the unique identifier of the order
     * @return the matching {@link Order}
     * @throws IllegalArgumentException if {@code orderID} is less than or equal to zero
     * @throws OrderNotFoundException if no order with the given ID exists
     * @throws RuntimeException if a database error occurs
     */
    Order findOrderByID(int orderID) throws OrderNotFoundException;

    /**
     * Retrieves all orders.
     *
     * @return a list of all orders (possibly empty)
     * @throws RuntimeException if a database error occurs
     */
    List<Order> findAllOrders();

    /**
     * Retrieves all orders with the given status.
     *
     * @param orderStatus the order status to filter by
     * @return a list of matching orders (possibly empty)
     * @throws IllegalArgumentException if {@code orderStatus} is null
     * @throws RuntimeException if a database error occurs
     */
    List<Order> findAllOrdersByStatus(OrderStatus orderStatus);

    /**
     * Retrieves all orders placed by a specific customer.
     *
     * @param customerID the unique identifier of the customer
     * @return a list of matching orders (possibly empty)
     * @throws IllegalArgumentException if {@code customerID} is less than or equal to zero
     * @throws RuntimeException if a database error occurs
     */
    List<Order> findAllOrdersByCustomer(int customerID);

    /**
     * Retrieves all items belonging to a specific order.
     *
     * @param orderID the unique identifier of the order
     * @return a list of order items (possibly empty)
     * @throws IllegalArgumentException if {@code orderID} is less than or equal to zero
     * @throws RuntimeException if a database error occurs
     */
    List<OrderItem> findAllOrderItemsByOrder(int orderID);

    /**
     * Updates the status of an existing order.
     * Performs:
     *     Validation of order ID and new status
     *     Verification of allowed status transitions (e.g., cannot ship a cancelled order)
     *
     *
     * @param orderID the unique identifier of the order
     * @param newOrderStatus the new status to set
     * @throws IllegalArgumentException if {@code orderID} is less than or equal to zero or {@code newOrderStatus} is null
     * @throws OrderNotFoundException if no order with the given ID exists
     * @throws RuntimeException if a database error occurs
     */
    void updateOrderStatus(int orderID, OrderStatus newOrderStatus) throws OrderNotFoundException;
}
