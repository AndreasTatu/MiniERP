package com.minierp.model.stock;

import java.util.Objects;

public class Stock {

    int stockID; //PK
    int productID; //FK from products
    String location; //in form of: A-01-02
    int quantity;
    int min_Stock;
    int max_Stock;
    int reserved;
    boolean active = true;

    public Stock(int stockID, int productID, String location, int quantity, int min_Stock, int max_Stock, int reserved, boolean active) {
        this.stockID = stockID;
        this.productID = productID;
        this.location = location;
        this.quantity = quantity;
        this.min_Stock = min_Stock;
        this.max_Stock = max_Stock;
        this.reserved = reserved;
        this.active = active;
    }

    public Stock(int productID, String location, int quantity, int min_Stock, int max_Stock, int reserved, boolean active) {
        this.productID = productID;
        this.location = location;
        this.quantity = quantity;
        this.min_Stock = min_Stock;
        this.max_Stock = max_Stock;
        this.reserved = reserved;
        this.active = active;
    }

    public int getStockID() {
        return stockID;
    }

    public void setStockID(int stockID) {
        this.stockID = stockID;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMin_Stock() {
        return min_Stock;
    }

    public void setMin_Stock(int min_Stock) {
        this.min_Stock = min_Stock;
    }

    public int getMax_Stock() {
        return max_Stock;
    }

    public void setMax_Stock(int max_Stock) {
        this.max_Stock = max_Stock;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return productID == stock.productID && Objects.equals(location, stock.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productID, location);
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockID=" + stockID +
                ", productID=" + productID +
                ", location='" + location + '\'' +
                ", quantity=" + quantity +
                ", min_Stock=" + min_Stock +
                ", max_Stock=" + max_Stock +
                ", reserved=" + reserved +
                ", active=" + active +
                '}';
    }

}