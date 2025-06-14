package model.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Order {

    private int orderID;
    private int customerID;
    private LocalDateTime orderDate;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal totalPrice;
    private OrderStatus orderStatus = OrderStatus.PENDING; //default status

    public Order(int customerID, LocalDateTime orderDate, BigDecimal discount, BigDecimal tax, BigDecimal totalPrice) {
        this.customerID = customerID;
        this.orderDate = orderDate;
        this.discount = discount;
        this.tax = tax;
        this.totalPrice = totalPrice;
    }

    public int getOrderID() {
        return orderID;
    }

    public int getCustomerID() {
        return customerID;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return customerID == order.customerID && Objects.equals(orderDate, order.orderDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerID, orderDate);
    }

}
