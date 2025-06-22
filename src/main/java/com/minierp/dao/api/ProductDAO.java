package com.minierp.dao.api;

import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.model.product.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductDAO {

    //CRUD-Methoden:

    //create
    void createProduct(Product product) throws ProductAlreadyExistsException, SQLException;

    //read
    Product findProductByID(int productID) throws ProductNotFoundException, SQLException;
    Product findProductByName(String name) throws ProductNotFoundException, SQLException;
    List<Product> findAllActiveProducts() throws SQLException;
    List<Product> findAllProducts() throws SQLException;

    //update
    void updateProduct(Product product) throws ProductNotFoundException, SQLException;

    //delete
    void deactivateProduct(int productID) throws ProductNotFoundException, SQLException;

}
