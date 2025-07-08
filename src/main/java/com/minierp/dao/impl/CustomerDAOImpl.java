package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.CustomerAlreadyExistsException;
import com.minierp.common.exceptions.CustomerNotFoundException;
import com.minierp.dao.api.CustomerDAO;
import com.minierp.model.customer.Customer;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAOImpl implements CustomerDAO {

    //create
    @Override
    public void createCustomer(Customer customer) throws CustomerAlreadyExistsException, SQLException{
        //SQL-String definition
        final String checkSQL = "SELECT 1 FROM customers WHERE LOWER(email) = LOWER(?)";
        final String insertSQL = "INSERT INTO customers (name, address, birthdate, email, phone, active) VALUES(?, ?, ?, ?, ?, ?)";

        //try-with-resources: DB-connection, prepared-statements
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

            checkStmt.setString(1, customer.getEmail()); //email gets inserted
            try(ResultSet rs = checkStmt.executeQuery()){             //statement gets executed and saved to rs

                if (rs.next()) {
                    throw new CustomerAlreadyExistsException("Customer with Email: " + customer.getEmail() + " already exists.");
                }
            }

            insertStmt.setString(1, customer.getName());
            insertStmt.setString(2, customer.getAddress());
            //nullable birthdate
            if ((customer.getBirthdate() != null)) {
                insertStmt.setDate(3, Date.valueOf(customer.getBirthdate()));
            } else insertStmt.setNull(3, Types.DATE);
            insertStmt.setString(4, customer.getEmail());
            insertStmt.setString(5, customer.getPhone());
            insertStmt.setBoolean(6, customer.isActive());

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Inserting customer failed, no rows affected.");
            }
        }
   }



    //read
    @Override
    public Customer findCustomerByID(int customerID) throws CustomerNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM customers WHERE customerID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)) {

            findStmt.setInt(1, customerID);
            try(ResultSet rs = findStmt.executeQuery()) {
                if(!rs.next()){
                    throw new CustomerNotFoundException("Customer with ID: " + customerID + "not found by customerID.");
                }

                //SQL-Date to Java-LocalDate
                //LocalDate birthdate = rs.getDate("birthdate").toLocalDate(); //not nullable version
                Date birthdateSQL = rs.getDate("birthdate"); //fetching SQL-Format-Date
                LocalDate birthdate = (birthdateSQL != null) ? birthdateSQL.toLocalDate() : null; //Conditional-Operator for nullable version

                // Extract all columns from ResultSet: customerID, name, address, birthdate, email, phone, active
            Customer customer = new Customer(
                    rs.getInt("customerID"),
                    rs.getString("name"),
                    rs.getString("address"),
                    birthdate,
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getBoolean("active"));

                return customer;
            }
        }
    }



    @Override
    public Customer findCustomerByEmail(String email) throws CustomerNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM customers WHERE LOWER(email) = LOWER(?)";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)) {

            findStmt.setString(1, email);

            try(ResultSet rs = findStmt.executeQuery()) {

                if(!rs.next()){
                    throw new CustomerNotFoundException("Customer with Email: " + email + " not found by email.");
                }
                // Extract all columns from ResultSet: customerID, name, address, birthdate, email, phone, active
                Date birthdateSQL = rs.getDate("birthdate");
                LocalDate birthdate = (birthdateSQL != null) ? birthdateSQL.toLocalDate() : null;

                Customer customer = new Customer(
                        rs.getInt("customerID"),
                        rs.getString("name"),
                        rs.getString("address"),
                        birthdate,
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getBoolean("active"));

                return customer;
            }
        }
    }



    @Override
    public List<Customer> findCustomerByNameContaining(String namePattern) throws SQLException{

        final String findSQL = "SELECT * FROM customers WHERE LOWER(name) LIKE LOWER(?) AND active = true"; //case-insensitive
        final List<Customer> customerList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)) {

            findStmt.setString(1,"%" + namePattern + "%");

            try(ResultSet rs = findStmt.executeQuery()) {
                while(rs.next()){

                    Date birthdateSQL = rs.getDate("birthdate");
                    LocalDate birthdate = (birthdateSQL != null) ? birthdateSQL.toLocalDate() : null;

                    Customer customer = new Customer(
                            rs.getInt("customerID"),
                            rs.getString("name"),
                            rs.getString("address"),
                            birthdate,
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getBoolean("active"));

                    customerList.add(customer);
                }
                return customerList;
            }
        }
    }



    @Override
    public List<Customer> findAllActiveCustomers() throws SQLException{

        final String findSQL = "SELECT * FROM customers WHERE active = true";
        final List<Customer> customerList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)) {

            try(ResultSet rs = findStmt.executeQuery()) {
                while(rs.next()){

                    Date birthdateSQL = rs.getDate("birthdate");
                    LocalDate birthdate = (birthdateSQL != null) ? birthdateSQL.toLocalDate() : null;

                    Customer customer = new Customer(
                            rs.getInt("customerID"),
                            rs.getString("name"),
                            rs.getString("address"),
                            birthdate,
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getBoolean("active"));

                    customerList.add(customer);
                }
                return customerList;
            }
        }
    }



    @Override
    public List<Customer> findAllCustomers() throws SQLException{

        final String findSQL = "SELECT * FROM customers";
        final List<Customer> customerList = new ArrayList<>();

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)) {

            try(ResultSet rs = findStmt.executeQuery()) {
                while(rs.next()){

                    Date birthdateSQL = rs.getDate("birthdate");
                    LocalDate birthdate = (birthdateSQL != null) ? birthdateSQL.toLocalDate() : null;

                    Customer customer = new Customer(
                            rs.getInt("customerID"),
                            rs.getString("name"),
                            rs.getString("address"),
                            birthdate,
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getBoolean("active"));

                    customerList.add(customer);
                }
                return customerList;
            }
        }
    }



    //update
    @Override
    public void updateCustomer(Customer customer) throws CustomerNotFoundException, CustomerAlreadyExistsException, SQLException{

        final String checkSQL = "SELECT 1 FROM customers WHERE LOWER(email) = LOWER(?) and customerID != ?";
        final String updateSQL = "UPDATE customers SET name = ?, address = ?, birthdate = ?, email = ?, phone = ?, active = ? WHERE customerID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            checkStmt.setString(1, customer.getEmail());
            checkStmt.setInt(2, customer.getCustomerID());
            try(ResultSet rs = checkStmt.executeQuery()){

                if(rs.next()){
                    throw new CustomerAlreadyExistsException("Customer with Email: " + customer.getEmail() + " found. Update failed");
                }
            }

            updateStmt.setString(1, customer.getName());
            updateStmt.setString(2, customer.getAddress());
            if(customer.getBirthdate() == null){
                updateStmt.setNull(3, Types.DATE);
            } else updateStmt.setDate(3, Date.valueOf(customer.getBirthdate()));
            updateStmt.setString(4, customer.getEmail());
            updateStmt.setString(5, customer.getPhone());
            updateStmt.setBoolean(6, customer.isActive());
            updateStmt.setInt(7, customer.getCustomerID());

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new CustomerNotFoundException("Customer with customerID: " + customer.getCustomerID() + " not found for update.");
            }
        }
    }



    //delete (soft-delete)->setActive(false)
    @Override
    public void deactivateCustomer(int customerID) throws CustomerNotFoundException, SQLException{

        final String updateSQL = "UPDATE customers SET active = ? WHERE customerID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            updateStmt.setBoolean(1, false);
            updateStmt.setInt(2, customerID);

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new CustomerNotFoundException("Deactivation failed: Customer with customerID: " + customerID + " not found.");
            }
        }
    }


    //delete (soft-delete)->setActive(true)
    @Override
    public void reactivateCustomer(int customerID) throws CustomerNotFoundException, SQLException{

        final String updateSQL = "UPDATE customers SET active = ? WHERE customerID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            updateStmt.setBoolean(1, true);
            updateStmt.setInt(2, customerID);

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new CustomerNotFoundException("Reactivation failed: Customer with customerID: " + customerID + " not found.");
            }
        }
    }


}
