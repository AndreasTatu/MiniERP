package com.minierp.model.product;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {

    private int productID;
    private String name;
    private String description; //nullable
    private BigDecimal weight;
    private BigDecimal price;
    private int minStockThreshold;
    private boolean active = true;

    public Product(int productID, String name, String description, BigDecimal weight, BigDecimal price, int minStockThreshold, boolean active) {
        this.productID = productID;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.price = price;
        this.minStockThreshold = minStockThreshold;
        this.active = active;
    }

    public Product(String name, String description, BigDecimal weight, BigDecimal price, int minStockThreshold) {
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.price = price;
        this.minStockThreshold = minStockThreshold;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getMinStockThreshold() {
        return minStockThreshold;
    }

    public void setMinStockThreshold(int minStockThreshold) {
        this.minStockThreshold = minStockThreshold;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productID=" + productID +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", weight=" + weight +
                ", price=" + price +
                ", minStockThreshold=" + minStockThreshold +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(name, product.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
