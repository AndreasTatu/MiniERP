package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.CustomerAlreadyExistsException;
import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.model.product.Product;

import java.sql.*;
import java.util.List;

public class ProductDAOImpl {

    //CRUD-Methoden:

    //create
    @Override
    void createProduct(Product product) throws ProductAlreadyExistsException, SQLException{

        //SQL-String definition
        String checkSQL = "SELECT 1 FROM products WHERE LOWER(email) = LOWER(?)";
        String insertSQL = "INSERT INTO products (name, address, birthdate, email, phone, active) VALUES(?, ?, ?, ?, ?, ?)";

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
    Product findProductByID(int productID) throws ProductNotFoundException, SQLException{

    }


    Product findProductByName(String name) throws ProductNotFoundException, SQLException{

    }


    List<Product> findAllActiveProducts() throws SQLException{

    }


    List<Product> findAllProducts() throws SQLException{

    }


    //update
    void updateProduct(Product product) throws ProductNotFoundException, SQLException{

    }


    //delete
    void deactivateProduct(int productID) throws ProductNotFoundException, SQLException{

    }


    void reactivateProduct(int productID) throws ProductNotFoundException, SQLException{

    }

}
