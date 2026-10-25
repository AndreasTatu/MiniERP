package com.minierp.common.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates the SQLite schema when starting a fresh local MiniERP database. */
public final class SchemaInitializer {
    private SchemaInitializer() {}

    public static void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        customerID INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        address TEXT,
                        birthdate TEXT,
                        email TEXT NOT NULL UNIQUE,
                        phone TEXT,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        productID INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        description TEXT,
                        weight NUMERIC NOT NULL,
                        price NUMERIC NOT NULL,
                        minStockThreshold INTEGER NOT NULL DEFAULT 0,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        orderID INTEGER PRIMARY KEY AUTOINCREMENT,
                        customerID INTEGER NOT NULL,
                        orderDateTime TEXT NOT NULL,
                        discount NUMERIC DEFAULT 0,
                        tax NUMERIC DEFAULT 0,
                        totalPrice NUMERIC NOT NULL,
                        orderStatus TEXT NOT NULL,
                        FOREIGN KEY (customerID) REFERENCES customers(customerID)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS orderItems (
                        orderItemID INTEGER PRIMARY KEY AUTOINCREMENT,
                        orderID INTEGER NOT NULL,
                        productID INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice NUMERIC NOT NULL,
                        FOREIGN KEY (orderID) REFERENCES orders(orderID),
                        FOREIGN KEY (productID) REFERENCES products(productID)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS stocks (
                        stockID INTEGER PRIMARY KEY AUTOINCREMENT,
                        productID INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 0,
                        maxStock INTEGER NOT NULL DEFAULT 0,
                        reserved INTEGER NOT NULL DEFAULT 0,
                        active INTEGER NOT NULL DEFAULT 1,
                        UNIQUE (productID, location),
                        FOREIGN KEY (productID) REFERENCES products(productID)
                    )
                    """);
        }
    }
}
