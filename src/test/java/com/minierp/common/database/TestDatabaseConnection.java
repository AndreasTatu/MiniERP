package com.minierp.common.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDatabaseConnection {

    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    static {
        try {
            // start H2 and load test-scheme
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize test schema", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    private static void initSchema() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            //tables-scheme
            stmt.execute("""
                CREATE TABLE customers (
                    customerID INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    address VARCHAR(255),
                    birthdate DATE,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    phone VARCHAR(255),
                    active BOOLEAN
                );

                CREATE TABLE orders (
                    orderID INT AUTO_INCREMENT PRIMARY KEY,
                    customerID INT NOT NULL,
                    orderDateTime DATETIME NOT NULL,
                    discount DECIMAL(3,2),
                    tax DECIMAL(3,2),
                    totalPrice DECIMAL(10,2),
                    orderStatus VARCHAR(50) NOT NULL,
                    FOREIGN KEY (customerID) REFERENCES customers(customerID)
                );
                
                 CREATE TABLE products (
                    productID INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(255),
                    weight DECIMAL(10,2) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    minStockThreshold INT,
                    active BOOLEAN
                );

                CREATE TABLE orderItems (
                    orderItemID INT AUTO_INCREMENT PRIMARY KEY,
                    orderID INT NOT NULL,
                    productID INT NOT NULL,
                    quantity INT NOT NULL,
                    unitPrice DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (orderID) REFERENCES orders(orderID),
                    FOREIGN KEY (productID) REFERENCES products(productID)
                );
                
                CREATE TABLE stocks (
                    stockID INT AUTO_INCREMENT PRIMARY KEY,
                    productID INT,
                    location VARCHAR(50),
                    quantity INT,
                    maxStock INT,
                    reserved INT,
                    active BOOLEAN,
                    UNIQUE (productID, location),
                    FOREIGN KEY (productID) REFERENCES products(productID)
                );
            """);
        }
    }
}
