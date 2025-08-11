package com.minierp.service.stock;

import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.common.exceptions.StockAlreadyExistsException;
import com.minierp.common.exceptions.StockNotFoundException;
import com.minierp.dao.product.ProductDAO;
import com.minierp.dao.stock.StockDAO;
import com.minierp.model.stock.Stock;

import java.sql.SQLException;
import java.util.List;

public class StockServiceImpl implements StockService{

    private final StockDAO stockDAO;
    private final ProductDAO productDAO;

    public StockServiceImpl(StockDAO stockDAO, ProductDAO productDAO) {
        this.stockDAO = stockDAO;
        this.productDAO = productDAO;
    }


    @Override
    public void createStock(Stock stock) throws StockAlreadyExistsException, ProductNotFoundException{

        if (stock == null){
            throw new IllegalArgumentException("stock-object must not be null");
        }

        if (stock.getProductID() <= 0)
            throw new IllegalArgumentException("Invalid productID: must be greater than 0");

        String location = stock.getLocation();
        if (location == null || location.isBlank())
            throw new IllegalArgumentException("Location must not be null or blank");

        if (!location.matches("[A-Z]-\\d{2}-\\d{2}"))
            throw new IllegalArgumentException("Location must match format 'A-01-02'");

        int quantity = stock.getQuantity();
        int maxStock = stock.getMaxStock();
        int reserved = stock.getReserved();

        if (quantity < 0)
            throw new IllegalArgumentException("Quantity must not be negative");

        if (maxStock <= 0)
            throw new IllegalArgumentException("maxStock must be greater than 0");

        if (quantity > maxStock)
            throw new IllegalArgumentException("Quantity must not exceed maxStock");

        if (reserved < 0)
            throw new IllegalArgumentException("Reserved amount must not be negative");

        if (reserved > quantity)
            throw new IllegalArgumentException("Reserved amount must not exceed quantity");

        //check product existence
        try{
            productDAO.findProductByID(stock.getProductID());
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error while checking product existence", e);
        }

        //create stock entry
        try{
            stockDAO.createStock(stock);
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }

    }


    @Override
    public Stock findStockByStockID(int stockID) throws StockNotFoundException {
        if (stockID <= 0) {
            throw new IllegalArgumentException("stockID must be greater than 0");
        }

        try {
            return stockDAO.findStockByStockID(stockID);
        } catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }
    }


    @Override
    public List<Stock> findStockByProductID(int productID){
        if (productID <= 0){
            throw new IllegalArgumentException("productID must be positive");
        }

        try {
            return stockDAO.findStockByProductID(productID);
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }
    }

    public List<Stock> findAllActiveStock() {
        try {
            return stockDAO.findAllActiveStock();
        } catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }
    }

    public List<Stock> findAllStock() {
        try {
            return stockDAO.findAllStock();
        } catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }
    }


    @Override
    public void updateFullStock(Stock stock) throws StockNotFoundException, ProductNotFoundException{

        if (stock == null)
            throw new IllegalArgumentException("stock-object must not be null");

        if (stock.getStockID() <= 0)
            throw new IllegalArgumentException("Invalid stockID: must be greater than 0");

        if (stock.getProductID() <= 0)
            throw new IllegalArgumentException("Invalid productID: must be greater than 0");

        String location = stock.getLocation();
        if (location == null || location.isBlank())
            throw new IllegalArgumentException("Location must not be null or blank");

        if (!location.matches("[A-Z]-\\d{2}-\\d{2}"))
            throw new IllegalArgumentException("Location must match format 'A-01-02'");

        int quantity = stock.getQuantity();
        int maxStock = stock.getMaxStock();
        int reserved = stock.getReserved();

        if (quantity < 0)
            throw new IllegalArgumentException("Quantity must not be negative");

        if (maxStock <= 0)
            throw new IllegalArgumentException("maxStock must be greater than 0");

        if (quantity > maxStock)
            throw new IllegalArgumentException("Quantity must not exceed maxStock");

        if (reserved < 0)
            throw new IllegalArgumentException("Reserved amount must not be negative");

        if (reserved > quantity)
            throw new IllegalArgumentException("Reserved amount must not exceed quantity");

        //check product existence
        try{
            productDAO.findProductByID(stock.getProductID());
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error while checking product existence", e);
        }

        //update stock entry
        try{
            stockDAO.updateFullStock(stock);
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }

    }


    @Override
    public void updateStockQuantityAndReserved(int stockID, int quantity, int reserved) throws StockNotFoundException{

        if (stockID <= 0)
            throw new IllegalArgumentException("stockID must be greater than 0");

        if (quantity < 0)
            throw new IllegalArgumentException("quantity must not be negative");

        int maxStock = findStockByStockID(stockID).getMaxStock();
        if (quantity > maxStock)
            throw new IllegalArgumentException("quantity must not exceed maxStock");

        if (reserved < 0)
            throw new IllegalArgumentException("reserved must not be negative");

        if (reserved > quantity)
            throw new IllegalArgumentException("reserved must not exceed quantity");

        try {
            stockDAO.updateStockQuantityAndReserved(stockID, quantity, reserved);
        } catch (SQLException e) {
            throw new RuntimeException("Database access error while updating stock quantity and reserved", e);
        }
    }


    @Override
    public void deactivateStock(int stockID) throws StockNotFoundException{

        if (stockID <= 0)
            throw new IllegalArgumentException("stockID must be positive");

        try{
            stockDAO.deactivateStock(stockID);
        } catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }

    }


    @Override
    public void reactivateStock(int stockID) throws StockNotFoundException{

        if (stockID <= 0)
            throw new IllegalArgumentException("stockID must be positive");

        try{
            stockDAO.reactivateStock(stockID);
        } catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }

    }




}
