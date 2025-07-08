package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.dao.api.StockDAO;
import com.minierp.model.stock.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockDAOImpl implements StockDAO {

    //CRUD-Methods:

    // Create
    @Override
    public void createStock(Stock stock) throws StockAlreadyExistsException, SQLException{

        final String checkSQL = "SELECT 1 FROM stocks WHERE productID = ? AND location = ?";
        final String insertSQL = "INSERT INTO stocks (productID, location, quantity, minStock, maxStock, reserved, active) VALUES(?, ?, ?, ?, ?, ?, ?)";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement insertStmt = conn.prepareStatement(insertSQL)){

            checkStmt.setInt(1, stock.getProductID());
            checkStmt.setString(2, stock.getLocation());
            try(ResultSet rs = checkStmt.executeQuery()){
                if(rs.next()){
                    throw new StockAlreadyExistsException("Stock already exists for productID: " + stock.getProductID() + " on location: " + stock.getLocation());
                }
            }
            insertStmt.setInt(1, stock.getProductID());
            insertStmt.setString(2, stock.getLocation());
            insertStmt.setInt(3, stock.getQuantity());
            insertStmt.setInt(4, stock.getMinStock());
            insertStmt.setInt(5, stock.getMaxStock());
            insertStmt.setInt(6, stock.getReserved());
            insertStmt.setBoolean(7, stock.isActive());

            int affectedRows = insertStmt.executeUpdate();
            if(affectedRows == 0){
                throw new SQLException("Inserting stock failed, no rows affected.");
            }
        }
    }



    // Read

    private final Stock mapResultSetToStock(ResultSet rs) throws SQLException{
        return new Stock(
                rs.getInt("StockID"),
                rs.getInt("productID"),
                rs.getString("location"),
                rs.getInt("quantity"),
                rs.getInt("minStock"),
                rs.getInt("maxStock"),
                rs.getInt("reserved"),
                rs.getBoolean("active"));
    }

    @Override
    public Stock findStockByStockID(int stockID) throws StockNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM stocks WHERE stockID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setInt(1, stockID);
            try(ResultSet rs = findStmt.executeQuery()){
                if(!rs.next()){
                    throw new StockNotFoundException("Stock with ID: " + stockID + " not found by stockID.");
                }
                return mapResultSetToStock(rs);
            }
        }

    }



    @Override
    public List<Stock> findStockByProductID(int productID) throws SQLException{

        final String findSQL = "SELECT * FROM stocks WHERE productID = ? AND active = true";

        final List<Stock> stockList= new ArrayList<>();
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            findStmt.setInt(1, productID);
            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    stockList.add(mapResultSetToStock(rs));
                }
                return stockList;
            }
        }

    }



    @Override
    public List<Stock> findAllActiveStock() throws SQLException{

        final String findSQL = "SELECT * FROM stocks WHERE active = true";

        final List<Stock> stockList= new ArrayList<>();
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    stockList.add(mapResultSetToStock(rs));
                }
                return stockList;
            }
        }

    }



    @Override
    public List<Stock> findAllStock() throws SQLException{

        final String findSQL = "SELECT * FROM stocks";

        final List<Stock> stockList= new ArrayList<>();
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement findStmt = conn.prepareStatement(findSQL)){

            try(ResultSet rs = findStmt.executeQuery()){
                while (rs.next()){
                    stockList.add(mapResultSetToStock(rs));
                }
                return stockList;
            }
        }

    }



    // Update
    @Override
    public void updateFullStock(Stock stock) throws StockNotFoundException, ProductNotFoundException, SQLException{

        final String checkSQL = "SELECT 1 FROM products WHERE productID = ?";
        final String updateSQL = "UPDATE stocks SET productID = ?, location = ?, quantity = ?, min_stock = ?, max_stock = ?, reserved = ?, active = ? WHERE stockID = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL)){

            //checking if I have a valid productID from products for the stock update, before building the updateStmt
            checkStmt.setInt(1, stock.getProductID());
            try(ResultSet rs = checkStmt.executeQuery()){
                if(!rs.next()){
                    throw new ProductNotFoundException("Product with ID: "+ stock.getProductID() + " not found. Stock update failed");
                }
            }
            updateStmt.setInt(1, stock.getProductID());
            updateStmt.setString(2, stock.getLocation());
            updateStmt.setInt(3, stock.getQuantity());
            updateStmt.setInt(4, stock.getMinStock());
            updateStmt.setInt(5, stock.getMaxStock());
            updateStmt.setInt(6, stock.getReserved());
            updateStmt.setBoolean(7, stock.isActive());
            updateStmt.setInt(8, stock.getStockID());

            int affectedRows = updateStmt.executeUpdate();
            if(affectedRows == 0){
                throw new StockNotFoundException("Stock with stockID: " + stock.getStockID() + " not found for update.");
            }
        }
    }



    public void updateStockQuantityAndReserved(int stockID, int quantity, int reserved) throws StockNotFoundException, SQLException{



    }



    // Delete (Soft Delete)
    public void deactivateStock(int stockID) throws StockNotFoundException, SQLException{



    }



    public void reactivateStock(int stockID) throws StockNotFoundException, SQLException{



    }


}
