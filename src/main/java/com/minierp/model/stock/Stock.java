package com.minierp.model.stock;

import java.util.Objects;

public class Stock {

    int stockID; //PK
    int productID; //FK from products
    String location; //in form of: A-01-02
    int quantity;
    int minStock;
    int maxStock;
    int reserved;
    boolean active = true;

    public Stock(int stockID, int productID, String location, int quantity, int minStock, int maxStock, int reserved, boolean active) {
        this.stockID = stockID;
        this.productID = productID;
        this.location = location;
        this.quantity = quantity;
        this.minStock = minStock;
        this.maxStock = maxStock;
        this.reserved = reserved;
        this.active = active;
    }

    public Stock(int productID, String location, int quantity, int minStock, int maxStock, int reserved, boolean active) {
        this.productID = productID;
        this.location = location;
        this.quantity = quantity;
        this.minStock = minStock;
        this.maxStock = maxStock;
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

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
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
                ", minStock=" + minStock +
                ", maxStock=" + maxStock +
                ", reserved=" + reserved +
                ", active=" + active +
                '}';
    }

}