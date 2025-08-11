package com.minierp.dao.product;

import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.model.product.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAOImpl implements ProductDAO {

    private final Connection connection;

    public ProductDAOImpl(Connection connection) {
        this.connection = connection;
    }

    //CRUD-Methoden:

    //create
    @Override
    public void createProduct(Product product) throws ProductAlreadyExistsException, SQLException{

        //SQL-String definition
        final String checkSQL = "SELECT 1 FROM products WHERE LOWER(name) = LOWER(?)";
        final String insertSQL = "INSERT INTO products (name, description, weight, price, minStockThreshold, active) VALUES(?, ?, ?, ?, ?, ?)";

        //try-with-resources: DB-connection, prepared-statements
        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {

            checkStmt.setString(1, product.getName()); //name gets inserted
            try(ResultSet rs = checkStmt.executeQuery()){             //statement gets executed and saved to rs

                if (rs.next()) {
                    throw new ProductAlreadyExistsException("Product with name: " + product.getName() + " already exists.");
                }
            }

            insertStmt.setString(1, product.getName());
            //nullable description
            if ((product.getDescription() != null)) {
                insertStmt.setString(2, product.getDescription());
            } else insertStmt.setNull(2, Types.VARCHAR);
            insertStmt.setBigDecimal(3, product.getWeight());
            insertStmt.setBigDecimal(4, product.getPrice());
            insertStmt.setInt(5, product.getMinStockThreshold());
            insertStmt.setBoolean(6, product.isActive());

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Inserting product failed, no rows affected.");
            }
        }
    }


    //read
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException{ //minimizing redundancy for find-methods
        return new Product(
                rs.getInt("productID"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("weight"),
                rs.getBigDecimal("price"),
                rs.getInt("minStockThreshold"),
                rs.getBoolean("active"));
    }

    @Override
    public Product findProductByID(int productID) throws ProductNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM products WHERE productID = ?";

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)) {

            findStmt.setInt(1, productID);

            try(ResultSet rs = findStmt.executeQuery()){
                if (!rs.next()){
                    throw new ProductNotFoundException("Product with ID: " + productID + " not found by productID.");
                }
                return mapResultSetToProduct(rs); //ResultSet to Product
            }
        }
    }


    @Override
    public Product findProductByName(String name) throws ProductNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM products WHERE LOWER(name) = LOWER(?)";

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

            findStmt.setString(1,name);
            try(ResultSet rs = findStmt.executeQuery()){
                if(!rs.next()){
                    throw new ProductNotFoundException("Product with name: " + name + " not found by name.");
                }

                return mapResultSetToProduct(rs);
            }
        }
    }


    @Override
    public List<Product> findAllActiveProducts() throws SQLException{

        final String findSQL = "SELECT * FROM products WHERE active = true";
        final List<Product> productList = new ArrayList<>();

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

            try(ResultSet rs = findStmt.executeQuery()){
                while(rs.next()){
                    productList.add(mapResultSetToProduct(rs));
                }
                return productList;
            }
        }
    }


    @Override
    public List<Product> findAllProducts() throws SQLException{

        final String findSQL = "SELECT * FROM products";
        final List<Product> productList = new ArrayList<>();

        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

            try(ResultSet rs = findStmt.executeQuery()){
                while(rs.next()){
                    productList.add(mapResultSetToProduct(rs));
                }
                return productList;
            }
        }
    }


    //update
    @Override
    public void updateProduct(Product product) throws ProductNotFoundException, ProductAlreadyExistsException, SQLException{

        final String checkSQL = "SELECT 1 FROM products WHERE LOWER(name) = LOWER(?) AND productID != ?";
        final String updateSQL = "UPDATE products SET name = ?, description = ?, weight = ?, price = ?, minStockThreshold = ?, active = ? WHERE productID = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

            checkStmt.setString(1, product.getName());
            checkStmt.setInt(2, product.getProductID());
            try(ResultSet rs = checkStmt.executeQuery()){
                if(rs.next()){
                    throw new ProductAlreadyExistsException("Product with name: " + product.getName() + " found. Update failed");
                }
            }
            updateStmt.setString(1, product.getName());
            if(product.getDescription() == null){
                updateStmt.setNull(2, Types.VARCHAR);
            } else updateStmt.setString(2, product.getDescription());
            updateStmt.setBigDecimal(3, product.getWeight());
            updateStmt.setBigDecimal(4, product.getPrice());
            updateStmt.setInt(5, product.getMinStockThreshold());
            updateStmt.setBoolean(6, product.isActive());
            updateStmt.setInt(7, product.getProductID());

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new ProductNotFoundException("Product with productID: " + product.getProductID() + " not found for update.");
            }
        }
    }


    //delete
    @Override
    public void deactivateProduct(int productID) throws ProductNotFoundException, SQLException{

        final String updateSQL = "UPDATE products SET active = ? WHERE productID = ?";

        try(PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {

            updateStmt.setBoolean(1, false);
            updateStmt.setInt(2, productID);

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new ProductNotFoundException("Deactivation failed: Product with productID: " + productID + " not found.");
            }
        }
    }


    @Override
    public void reactivateProduct(int productID) throws ProductNotFoundException, SQLException{

        final String updateSQL = "UPDATE products SET active = ? WHERE productID = ?";

        try(PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {

            updateStmt.setBoolean(1, true);
            updateStmt.setInt(2, productID);

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new ProductNotFoundException("Reactivation failed: Product with productID: " + productID + " not found.");
            }
        }
    }


}
