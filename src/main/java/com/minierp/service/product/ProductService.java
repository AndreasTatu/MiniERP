package com.minierp.service.product;

import com.minierp.common.exceptions.ProductAlreadyExistsException;
import com.minierp.common.exceptions.ProductNotFoundException;
import com.minierp.model.product.Product;

import java.util.List;

public interface ProductService {

    //create
    /**
     * Creates a new product in the system after validating its fields.
     *
     * <p>This method ensures that the product has a non-empty name,
     * non-null price and weight. A product with a duplicate name
     * will be rejected.</p>
     *
     * @param product the product to be created (must not be null)
     * @throws IllegalArgumentException if the product or required fields are invalid
     * @throws ProductAlreadyExistsException if a product with the same name already exists
     */
    void createProduct(Product product) throws ProductAlreadyExistsException;



    //read
    /**
     * Retrieves a product by its unique product ID.
     *
     * <p>The product ID must be a positive integer. If no product
     * with the given ID exists, a {@link ProductNotFoundException} is thrown.</p>
     *
     * @param productID the unique identifier of the product (must be positive)
     * @return the matching {@link Product}
     * @throws IllegalArgumentException if the productID is less than or equal to zero
     * @throws ProductNotFoundException if no product with the given ID exists
     */
    Product findProductByID(int productID) throws ProductNotFoundException;



    /**
     * Retrieves a product by its unique name (case-insensitive).
     *
     * <p>The product name must be non-null and not blank.
     * The search is case-insensitive. If no product with the given
     * name exists, a {@link ProductNotFoundException} is thrown.</p>
     *
     * @param name the name of the product to search for (must not be null or blank)
     * @return the matching {@link Product}
     * @throws IllegalArgumentException if the name is null or blank
     * @throws ProductNotFoundException if no product with the given name exists
     */
    Product findProductByName(String name) throws ProductNotFoundException;



    /**
     * Retrieves all products that are currently marked as active.
     *
     * <p>This method returns an empty list if no active products exist.
     * It never returns {@code null}.</p>
     *
     * @return a list of active {@link Product} instances (never {@code null})
     * @throws RuntimeException if a database access error occurs
     */
    List<Product> findAllActiveProducts();



    //update
    /**
     * Updates the data of an existing product.
     *
     * <p>This method validates that the product object is not null and contains
     * a valid ID, name, price, and weight. The product name must be unique
     * across all other products. Description is optional and may be null.</p>
     *
     * @param product the product with updated values (must not be null)
     * @throws IllegalArgumentException if the product or its required fields are invalid
     * @throws ProductNotFoundException if no product with the given ID exists
     * @throws ProductAlreadyExistsException if another product with the same name already exists
     */
    void updateProduct(Product product) throws ProductNotFoundException, ProductAlreadyExistsException;



    //(soft-)delete
    /**
     * Marks the specified product as inactive (soft delete).
     *
     * <p>The product remains in the database but will be excluded from
     * active product queries. The product ID must be positive.</p>
     *
     * @param productID the ID of the product to deactivate (must be positive)
     * @throws IllegalArgumentException if the productID is less than or equal to zero
     * @throws ProductNotFoundException if no product with the given ID exists
     */
    void deactivateProduct(int productID) throws ProductNotFoundException;

    //undo deactivation
    void reactivateProduct(int productID) throws ProductNotFoundException;

}
