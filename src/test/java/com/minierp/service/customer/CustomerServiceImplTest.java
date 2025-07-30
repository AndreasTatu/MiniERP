package com.minierp.service.customer;

import com.minierp.common.database.TestDatabaseConnection;
import com.minierp.dao.customer.CustomerDAOImpl;
import com.minierp.model.customer.Customer;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceImplTest {

    private static Connection connection;
    private CustomerService customerService;

    @BeforeAll
    static void setupConnection() throws SQLException {
        connection = TestDatabaseConnection.getConnection();
    }

    @BeforeEach
    void setup() {
        customerService = new CustomerServiceImpl(new CustomerDAOImpl(connection));
    }

    @Test
    void testCreateAndFindCustomerByEmail() throws Exception {
        Customer customer = new Customer("Max Mustermann", "Hauptstraße 1", LocalDate.of(1990, 1, 1), "max@example.com", "0123456789");
        customerService.createCustomer(customer);

        Customer result = customerService.findCustomerByEmail("max@example.com");
        assertNotNull(result);
        assertEquals("Max Mustermann", result.getName());
        assertEquals("0123456789", result.getPhone());
    }

    @AfterEach
    void cleanup() throws SQLException {
        connection.createStatement().execute("DELETE FROM customers");
    }
}
