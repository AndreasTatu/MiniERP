package com.minierp.dao.api;

import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.model.stock.Stock;

import java.sql.SQLException;
import java.util.List;

public interface StockDAO {

    //CRUD-Methods

    // Create
    void createStock(Stock stock) throws StockAlreadyExistsException, SQLException;

    // Read
    Stock findStockByLocationID(String locationID) throws StockNotFoundException, SQLException;
    List<Stock> findStockByProductID(int productID) throws SQLException;
    List<Stock> findAllActiveStock() throws SQLException;
    List<Stock> findAllStock() throws SQLException;

    // Update
    void updateFullStock(Stock stock) throws StockNotFoundException, SQLException;
    void updateStockQuantityAndReserved(String locationID, int quantity, int reserved) throws StockNotFoundException, SQLException;

    // Delete (Soft Delete)
    void deactivateStock(String locationID) throws StockNotFoundException, SQLException;
    void reactivateStock(String locationID) throws StockNotFoundException, SQLException;
}
