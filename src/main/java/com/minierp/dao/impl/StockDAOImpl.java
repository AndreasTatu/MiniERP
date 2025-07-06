package com.minierp.dao.impl;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.model.stock.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StockDAOImpl {

    //CRUD-Methods:

    // Create
    public void createStock(Stock stock) throws StockAlreadyExistsException, SQLException{

        String checkSQL = "SELECT 1 FROM stocks WHERE productID = ? AND location = ?";
        String insertSQL = "INSERT INTO stocks (productID, location, quantity, min_stock, max_stock, reserved, active) VALUES(?, ?, ?, ?, ?, ?, ?)";

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
            insertStmt.setInt(4, stock.getMin_Stock());
            insertStmt.setInt(5, stock.getMax_Stock());
            insertStmt.setInt(6, stock.getReserved());
            insertStmt.setBoolean(7, stock.isActive());

            int affectedRows = insertStmt.executeUpdate();
            if(affectedRows == 0){
                throw new SQLException("Inserting stock failed, no rows affected.");
            }
        }
    }



    // Read

    private Product mapResultSetToStock

    public Stock findStockByStockID(String stockID) throws StockNotFoundException, SQLException{



    }



    public List<Stock> findStockByProductID(int productID) throws SQLException{



    }



    public List<Stock> findAllActiveStock() throws SQLException{



    }



    public List<Stock> findAllStock() throws SQLException{



    }



    // Update
    public void updateFullStock(Stock stock) throws StockNotFoundException, SQLException{



    }



    public void updateStockQuantityAndReserved(String stockID, int quantity, int reserved) throws StockNotFoundException, SQLException{



    }



    // Delete (Soft Delete)
    public void deactivateStock(String stockID) throws StockNotFoundException, SQLException{



    }



    public void reactivateStock(String stockID) throws StockNotFoundException, SQLException{



    }


}
