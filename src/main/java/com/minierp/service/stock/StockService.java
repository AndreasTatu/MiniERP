package com.minierp.service.stock;

import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.model.stock.Stock;

import java.sql.SQLException;
import java.util.List;

public interface StockService {

    //////////////////////////////       Create       ////////////////////////////////////
    /**
     * Creates a new stock entry after validating input and checking business constraints.
     *
     * @param stock the Stock object to create
     * @throws IllegalArgumentException if validation fails
     * @throws StockAlreadyExistsException if a stock entry with the same productID and location already exists
     * @throws SQLException if a database error occurs
     */
    void createStock(Stock stock) throws StockAlreadyExistsException, SQLException;



    ////////////////////////////////////     Read     /////////////////////////////////////
    /**
     * Retrieves a stock entry by its stockID.
     *
     * @param stockID the unique database identifier
     * @return the matching Stock entry
     * @throws StockNotFoundException if the stock entry is not found
     * @throws SQLException if a database error occurs
     */
    Stock findStockByStockID(int stockID) throws StockNotFoundException, SQLException;


    /**
     * Retrieves all stock entries for a given productID.
     *
     * @param productID the product's foreign key
     * @return list of active stock entries for that product
     * @throws SQLException if a database error occurs
     */
    List<Stock> findStockByProductID(int productID) throws SQLException;


    /**
     * Retrieves all active stock entries in the system.
     *
     * @return list of active stock entries
     * @throws SQLException if a database error occurs
     */
    List<Stock> findAllActiveStock() throws SQLException;


    /**
     * Retrieves all stock entries regardless of their active status.
     *
     * @return list of all stock entries
     * @throws SQLException if a database error occurs
     */
    List<Stock> findAllStock() throws SQLException;



    ////////////////////////////////       Update       /////////////////////////////////////////
    /**
     * Updates a stock entry completely after validating business rules.
     *
     * @param stock the updated Stock object
     * @throws IllegalArgumentException if validation fails
     * @throws StockNotFoundException if the stock entry does not exist
     * @throws ProductNotFoundException if the referenced product does not exist
     * @throws SQLException if a database error occurs
     */
    void updateFullStock(Stock stock) throws StockNotFoundException, ProductNotFoundException, SQLException;


    /**
     * Adjusts the quantity and reserved values for a given stock entry.
     *
     * @param stockID the unique stock identifier
     * @param quantity the new quantity
     * @param reserved the new reserved amount
     * @throws IllegalArgumentException if quantity < reserved
     * @throws StockNotFoundException if the stock entry does not exist
     * @throws SQLException if a database error occurs
     */
    void updateStockQuantityAndReserved(int stockID, int quantity, int reserved) throws StockNotFoundException, SQLException;


    ///////////////////////////////       (Soft-) Delete      //////////////////////////////////////
    /**
     * Performs a soft delete (deactivation) of the stock entry.
     *
     * @param stockID the ID of the stock to deactivate
     * @throws StockNotFoundException if the stock entry does not exist
     * @throws SQLException if a database error occurs
     */
    void deactivateStock(int stockID) throws StockNotFoundException, SQLException;


    /**
     * Reactivates a previously deactivated stock entry.
     *
     * @param stockID the ID of the stock to reactivate
     * @throws StockNotFoundException if the stock entry does not exist
     * @throws SQLException if a database error occurs
     */
    void reactivateStock(int stockID) throws StockNotFoundException, SQLException;

}
