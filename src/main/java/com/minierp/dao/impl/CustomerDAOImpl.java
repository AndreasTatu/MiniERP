package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.CustomerAlreadyExistsException;
import com.minierp.common.exceptions.CustomerNotFoundException;
import com.minierp.dao.api.CustomerDAO;
import com.minierp.model.customer.Customer;

import java.sql.*;
import java.util.List;

public class CustomerDAOImpl implements CustomerDAO {

    //create
    /*  sql-statements:
        customerAlreadyExists(email)
        SELECT COUNT(*) FROM customers WHERE email = ?

        createCustomer
        INSERT INTO customers (name, address, birthdate, email, phone, active)
        VALUES(?, ?, ?, ?, ?, ?)
     */
    @Override
    public void createCustomer(Customer customer) throws CustomerAlreadyExistsException, SQLException{
        //SQL-String definition
        String checkSQL = "SELECT COUNT(*) FROM customers WHERE email = ?";
        String insertSQL = "INSERT INTO customers (name, address, birthdate, email, phone, active) VALUES(?, ?, ?, ?, ?, ?)";

        //try-with-resources: DB-connection, prepared-statements
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

            checkStmt.setString(1, customer.getEmail()); //email gets inserted
            try(ResultSet rs = checkStmt.executeQuery()){             //statement gets executed and saved to rs

                if (rs.next() && rs.getInt(1) > 0) {
                    throw new CustomerAlreadyExistsException("Customer with Email: " + customer.getEmail() + " already exists.");
                }
            }

            insertStmt.setString(1, customer.getName());
            insertStmt.setString(2, customer.getAddress());
            insertStmt.setDate(3, Date.valueOf(customer.getBirthdate()));
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

    }



    @Override
    public Customer findCustomerByEmail(String email) throws CustomerNotFoundException, SQLException{

    }



    @Override
    public List<Customer> findCustomerByNameContaining(String namePattern) throws SQLException{

    }



    @Override
    public List<Customer> findAllActiveCustomers() throws SQLException{

    }



    @Override
    public List<Customer> findAllCustomers() throws SQLException{

    }



    //update
    @Override
    public void updateCustomer(Customer customer) throws CustomerNotFoundException, SQLException{

    }



    //delete (soft-delete)
    @Override
    public void deactivateCustomer(int customerID) throws CustomerNotFoundException, SQLException{

    } //setActive(false)


}
