package com.minierp.service.customer;

import com.minierp.common.exceptions.CustomerAlreadyExistsException;
import com.minierp.common.exceptions.CustomerNotFoundException;
import com.minierp.model.customer.Customer;

import java.sql.SQLException;
import java.util.List;

/**
 * Provides high-level customer-related business operations.
 * Acts as a bridge between controllers/UI and the DAO layer.
 */
public interface CustomerService {

    /**
     * Creates a new customer.
     *
     * @param customer The customer object to persist.
     * @throws CustomerAlreadyExistsException if a customer with the same email already exists.
     */
    void createCustomer(Customer customer) throws CustomerAlreadyExistsException;

    /**
     * Retrieves a customer by their unique ID.
     *
     * @param customerID The customer's ID.
     * @return The found Customer.
     * @throws CustomerNotFoundException if the customer was not found.
     */
    Customer findCustomerByID(int customerID) throws CustomerNotFoundException;


    /**
     * Retrieves a customer by their email address (case-insensitive).
     *
     * @param email The customer's email.
     * @return The found Customer.
     * @throws CustomerNotFoundException if the customer was not found.
     */
    Customer findCustomerByEmail(String email) throws CustomerNotFoundException;


    /**
     * Searches for active customers whose names contain the given pattern.
     *
     * @param namePattern The search pattern (case-insensitive).
     * @return List of matching active customers.
     */
    List<Customer> findCustomerByNameContaining(String namePattern);


    /**
     * Retrieves only currently active customers.
     *
     * @return List of active customers.
     */
    List<Customer> findAllActiveCustomers();


    /**
     * Updates the information of an existing customer.
     *
     * @param customer The customer object with updated data.
     * @throws CustomerNotFoundException if the customer does not exist.
     * @throws CustomerAlreadyExistsException if the email is used by another customer.
     */
    void updateCustomer(Customer customer) throws CustomerNotFoundException, CustomerAlreadyExistsException;


    /**
     * De-/Reactivates a customer (soft delete).
     *
     * @param customerID The ID of the customer to de-/reactivate.
     * @throws CustomerNotFoundException if the customer does not exist.
     */
    void deactivateCustomer(int customerID) throws CustomerNotFoundException; //setActive(false)
    void reactivateCustomer(int customerID) throws CustomerNotFoundException; //setActive(true)


}
