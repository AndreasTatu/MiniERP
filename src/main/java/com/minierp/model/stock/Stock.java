package com.minierp.model.stock;

import java.util.Objects;

public class Stock {

    String locationID; //Lagerplatz in Form von Gang-Regal-Fach zb.: A-01-02
    int productID;
    int quantity;
    int min_Stock;
    int max_Stock;
    int reserved;

    public Stock(String locationID, int productID, int quantity, int min_Stock, int max_Stock, int reserved) {
        this.locationID = locationID;
        this.productID = productID;
        this.quantity = quantity;
        this.min_Stock = min_Stock;
        this.max_Stock = max_Stock;
        this.reserved = reserved;
    }

    public String getLocationID() {
        return locationID;
    }

    public void setLocationID(String locationID) {
        this.locationID = locationID;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(locationID, stock.locationID);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locationID);
    }

    @Override
    public String toString() {
        return "Stock{" +
                "locationID='" + locationID + '\'' +
                ", productID=" + productID +
                ", quantity=" + quantity +
                ", min_Stock=" + min_Stock +
                ", max_Stock=" + max_Stock +
                ", reserved=" + reserved +
                '}';
    }
}