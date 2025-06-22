package minierp.model.product;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {

    private int productID;
    private String name;
    private String description;
    private BigDecimal weight;
    private BigDecimal price;

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

    @Override
    public String toString() {
        return "Product{" +
                "productID=" + productID +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", weight=" + weight +
                ", price=" + price +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(name, product.name) &&
               Objects.equals(description, product.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
