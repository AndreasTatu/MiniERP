package com.minierp.service.customer;

import com.minierp.common.exceptions.CustomerAlreadyExistsException;
import com.minierp.common.exceptions.CustomerNotFoundException;
import com.minierp.dao.customer.CustomerDAO;
import com.minierp.model.customer.Customer;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of the CustomerService interface.
 * Handles validation, business rules, and delegates to the DAO layer.
 */
public class CustomerServiceImpl implements CustomerService{

    private final CustomerDAO customerDAO;

    /**
     * Constructs a CustomerServiceImpl with the given CustomerDAO.
     *
     * @param customerDAO The DAO used for persistence operations.
     */
    public CustomerServiceImpl(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public void createCustomer(Customer customer) throws CustomerAlreadyExistsException{

        if (customer == null) {
            throw new IllegalArgumentException("Customer must not be null.");
        }

        //trim and validation using conditional operator
        //null converts to empty string so .isEmpty throws Exception in both cases
        String name = customer.getName() != null ? customer.getName().trim() : "";
        String address = customer.getAddress() != null ? customer.getAddress().trim() : "";
        String email = customer.getEmail() != null ? customer.getEmail().trim() : "";
        String phone = customer.getPhone() != null ? customer.getPhone().trim() : "";

        if (name.isEmpty() || address.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            throw new IllegalArgumentException("Name, address, email and phone must not be empty.");
        }

        //mapping the values to a customer object
        customer.setName(name);
        customer.setAddress(address);
        customer.setEmail(email);
        customer.setPhone(phone);

        //validating the format of the given email via regex
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        //customer gets handed to the DAO-method
        try{
        customerDAO.createCustomer(customer);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public Customer findCustomerByID(int customerID) throws CustomerNotFoundException {

        if (customerID <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }

        try{
        return customerDAO.findCustomerByID(customerID);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public Customer findCustomerByEmail(String email) throws CustomerNotFoundException {

        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email must not be null or empty.");
        }

        email = email.trim();

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        try{
        return customerDAO.findCustomerByEmail(email);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public List<Customer> findCustomerByNameContaining(String namePattern) {

        if (namePattern == null || namePattern.trim().isEmpty()){
            throw new IllegalArgumentException("NamePattern must not be null or empty.");
        }

        namePattern = namePattern.trim();

        try{
        return customerDAO.findCustomerByNameContaining(namePattern);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public List<Customer> findAllActiveCustomers() {

        try{
        return customerDAO.findAllActiveCustomers();}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public void updateCustomer(Customer customer) throws CustomerNotFoundException, CustomerAlreadyExistsException {

        if (customer == null){
            throw new IllegalArgumentException("Customer must not be null");
        }

        String name = customer.getName() != null ? customer.getName().trim() : "";
        String address = customer.getAddress() != null ? customer.getAddress().trim() : "";
        String email = customer.getEmail() != null ? customer.getEmail().trim() : "";
        String phone = customer.getPhone() != null ? customer.getPhone().trim() : "";

        if (name.isEmpty() || address.isEmpty() || email.isEmpty() || phone.isEmpty()){
            throw new IllegalArgumentException("Name, address, email and phone must not be empty.");
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        customer.setName(name);
        customer.setAddress(address);
        customer.setEmail(email);
        customer.setPhone(phone);

        try{
        customerDAO.updateCustomer(customer);} //DAO checks for E-Mail-collision with different ID

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public void deactivateCustomer(int customerID) throws CustomerNotFoundException{

        if (customerID <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }

        try{
        customerDAO.deactivateCustomer(customerID);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


    @Override
    public void reactivateCustomer(int customerID) throws CustomerNotFoundException{

        if (customerID <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }

        try{
        customerDAO.reactivateCustomer(customerID);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}
    }


}
