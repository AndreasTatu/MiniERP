package com.minierp.dao.stock;

import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.model.stock.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockDAOImpl implements StockDAO {

    private final Connection connection;

    public StockDAOImpl(Connection connection) {
        this.connection = connection;
    }

    //CRUD-Methods:

    // Create
    @Override
    public void createStock(Stock stock) throws StockAlreadyExistsException, SQLException{

        final String checkSQL = "SELECT 1 FROM stocks WHERE productID = ? AND location = ?";
        final String insertSQL = "INSERT INTO stocks (productID, location, quantity, maxStock, reserved, active) VALUES(?, ?, ?, ?, ?, ?)";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement insertStmt = connection.prepareStatement(insertSQL)){

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
                rs.getInt("maxStock"),
                rs.getInt("reserved"),
                rs.getBoolean("active"));
    }

    @Override
    public Stock findStockByStockID(int stockID) throws StockNotFoundException, SQLException{

        final String findSQL = "SELECT * FROM stocks WHERE stockID = ?";

        try (PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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
        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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
        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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
        try(PreparedStatement findStmt = connection.prepareStatement(findSQL)){

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
        final String updateSQL = "UPDATE stocks SET productID = ?, location = ?, quantity = ?, max_stock = ?, reserved = ?, active = ? WHERE stockID = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

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



    @Override
    public void updateStockQuantityAndReserved(int stockID, int quantity, int reserved) throws StockNotFoundException, SQLException{

        final String checkSQL = "SELECT 1 FROM stocks WHERE stockID = ?";
        final String updateSQL = "UPDATE stocks SET quantity = ?, reserved = ? WHERE stockID = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

            //existence check
            checkStmt.setInt(1, stockID);
            try(ResultSet rs = checkStmt.executeQuery()){
                if (!rs.next()){
                    throw new StockNotFoundException("Update failed: stock with ID: " + stockID + " not found.");
                }
            }
            //perform update
            updateStmt.setInt(1, quantity);
            updateStmt.setInt(2, reserved);
            updateStmt.setInt(3, stockID);

            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Update failed: no Rows affected.");
            }
        }
    }



    // Delete (Soft Delete)
    @Override
    public void deactivateStock(int stockID) throws StockNotFoundException, SQLException{

        final String checkSQL = "SELECT 1 FROM stocks WHERE stockID = ?";
        final String updateSQL = "UPDATE stocks SET active = false WHERE stockID = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

            //existence check
            checkStmt.setInt(1, stockID);
            try(ResultSet rs = checkStmt.executeQuery()){
                if (!rs.next()){
                    throw new StockNotFoundException("Deactivation failed: stock with ID: " + stockID + " not found.");
                }
            }
            //perform update
            updateStmt.setInt(1, stockID);

            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Deactivation failed: no Rows affected.");
            }
        }
    }



    @Override
    public void reactivateStock(int stockID) throws StockNotFoundException, SQLException{

        final String checkSQL = "SELECT 1 FROM stocks WHERE stockID = ?";
        final String updateSQL = "UPDATE stocks SET active = false WHERE stockID = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkSQL);
            PreparedStatement updateStmt = connection.prepareStatement(updateSQL)){

            //existence check
            checkStmt.setInt(1, stockID);
            try(ResultSet rs = checkStmt.executeQuery()){
                if (!rs.next()){
                    throw new StockNotFoundException("Reactivation failed: stock with ID: " + stockID + " not found.");
                }
            }
            //perform update
            updateStmt.setInt(1, stockID);

            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0){
                throw new SQLException("Reactivation failed: no Rows affected.");
            }
        }
    }


}
