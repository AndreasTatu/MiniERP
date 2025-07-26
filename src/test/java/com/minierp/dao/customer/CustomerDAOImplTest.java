package com.minierp.dao.customer;

import com.minierp.common.database.TestDatabaseConnection;
import com.minierp.model.customer.Customer;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDAOImplTest {

    private static Connection connection;
    private static CustomerDAO customerDAO;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        connection = TestDatabaseConnection.getConnection();
        customerDAO = new CustomerDAOImpl(connection);
    }

    @AfterEach
    void clearTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM customers");
        }
    }

    @Test
    void createCustomer_savesCustomerCorrectly() throws SQLException {
        // Arrange
        Customer customer = new Customer();
        customer.setName("Hans Test");
        customer.setAddress("Teststraße 1");
        customer.setBirthdate(LocalDate.of(1990, 1, 1));
        customer.setEmail("hans@example.com");
        customer.setPhone("123456789");
        //customer.setActive(true);

        // Act
        customerDAO.createCustomer(customer);

        // Assert
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM customers WHERE email = ?")) {
            stmt.setString(1, "hans@example.com");
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Customer should exist in database");

            assertEquals("Hans Test", rs.getString("name"));
            assertEquals("Teststraße 1", rs.getString("address"));
            assertEquals("123456789", rs.getString("phone"));
            assertEquals(Date.valueOf(LocalDate.of(1990, 1, 1)), rs.getDate("birthdate"));
            assertTrue(rs.getBoolean("active"));
        }
    }
}

