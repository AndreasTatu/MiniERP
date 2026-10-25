package com.minierp.service.order;

import com.minierp.dao.order.OrderDAO;
import com.minierp.dao.product.ProductDAO;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;

    public OrderServiceImpl(OrderDAO orderDAO, ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
    }

    @Override
    public void createOrder(Order order, List<OrderItem> orderItems){

        if (order == null)
            throw new IllegalArgumentException("Order-object must not be null");

        if (orderItems == null || orderItems.isEmpty())
            throw new IllegalArgumentException("Order must contain at least one OrderItem");

        if (order.getCustomerID() <= 0)
            throw new IllegalArgumentException("customerID must be positive.");

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



    }

}
