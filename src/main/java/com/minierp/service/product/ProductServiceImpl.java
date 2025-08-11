package com.minierp.service.product;

import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.dao.product.ProductDAO;
import com.minierp.model.product.Product;

import java.sql.SQLException;
import java.util.List;

public class ProductServiceImpl implements ProductService {

    private final ProductDAO productDAO;

    public ProductServiceImpl(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    //create
    @Override
    public void createProduct(Product product) throws ProductAlreadyExistsException{

        if (product == null){
            throw new IllegalArgumentException("Product-object must not be null.");
        }

        String name = product.getName() != null ? product.getName().trim() : "";

        if (name.isEmpty() || product.getPrice() == null || product.getWeight() == null ){
            throw new IllegalArgumentException("Name, Price, Weight must not be empty or null.");
        }

        try{
            productDAO.createProduct(product);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }

    }


    //read
    @Override
    public Product findProductByID(int productID) throws ProductNotFoundException{

        if (productID <= 0){
            throw new IllegalArgumentException("productID must be positive.");
        }

        try {
            return productDAO.findProductByID(productID);}

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);
        }
    }


    @Override
    public Product findProductByName(String name) throws ProductNotFoundException{

        if (name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Name must not be null or empty.");
        }

        name = name.trim();

        try {
           return productDAO.findProductByName(name);
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}

    }


    public List<Product> findAllActiveProducts(){

        try {
            return productDAO.findAllActiveProducts();
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}

    }


    //update
    @Override
    public void updateProduct(Product product) throws ProductNotFoundException, ProductAlreadyExistsException{

        if(product == null){
            throw new IllegalArgumentException("Product-object must not be null");
        }

        String name = product.getName() != null ? product.getName().trim() : "";

        if (name.isEmpty() || product.getPrice() == null || product.getWeight() == null ){
            throw new IllegalArgumentException("Name, Price, Weight must not be empty or null.");
        }

        //DAO checks if the updated name collides with other product

        try{
        productDAO.updateProduct(product);
        }
        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}

    }


    //(soft-)delete
    public void deactivateProduct(int productID) throws ProductNotFoundException{

        if (productID <= 0){
            throw new IllegalArgumentException("productID must be positive");
        }

        try{
            productDAO.deactivateProduct(productID);
        }

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}

    }


    public void reactivateProduct(int productID) throws ProductNotFoundException{

        if (productID <= 0){
            throw new IllegalArgumentException("productID must be positive");
        }

        try{
            productDAO.reactivateProduct(productID);
        }

        catch (SQLException e) {
            throw new RuntimeException("Database access error", e);}

    }

}
