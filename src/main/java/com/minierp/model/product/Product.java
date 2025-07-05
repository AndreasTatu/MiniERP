package com.minierp.model.product;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {

    private int productID;
    private String name;
    private String description; //nullable
    private BigDecimal weight;
    private BigDecimal price;
    private boolean active = true;

    public Product(int productID, String name, String description, BigDecimal weight, BigDecimal price, boolean active) {
        this.productID = productID;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.price = price;
        this.active = active;
    }

    public Product(String name, String description, BigDecimal weight, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.price = price;
    }

    public int getProductID() {
        return productID;
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
