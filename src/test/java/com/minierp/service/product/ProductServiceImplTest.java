package com.minierp.service.product;

import com.minierp.common.database.TestDatabaseConnection;
import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.dao.product.ProductDAO;
import com.minierp.dao.product.ProductDAOImpl;
import com.minierp.model.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceImplTest {

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() throws Exception {
        // Datenbank aufräumen vor jedem Test (alle Tabellen leeren)
        try (Connection conn = TestDatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM orderItems");
            stmt.executeUpdate("DELETE FROM orders");
            stmt.executeUpdate("DELETE FROM stocks");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM customers");
        }

        // DAO & Service initialisieren
        Connection testConnection = TestDatabaseConnection.getConnection();
        ProductDAO productDAO = new ProductDAOImpl(testConnection);

        productService = new ProductServiceImpl(productDAO);
    }

    @Test
    void updateProduct_shouldThrowException_whenNameAlreadyExists() {
        // Arrange
        Product productA = new Product("Widget A", "desc A", new BigDecimal("1.5"), new BigDecimal("9.99"), 5);
        Product productB = new Product("Widget B", "desc B", new BigDecimal("2.0"), new BigDecimal("19.99"), 5);

        productService.createProduct(productA);
        productService.createProduct(productB);

        // Re-fetch productB to get its ID
        Product existingB = productService.findProductByName("Widget B");
        existingB.setName("Widget A"); // now duplicate name

        // Act + Assert
        assertThrows(ProductAlreadyExistsException.class, () -> {
            productService.updateProduct(existingB);
        });
    }
}
