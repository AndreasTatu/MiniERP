package minierp.dao.api.customer;

import minierp.common.exceptions.CustomerAlreadyExistsException;
import minierp.common.exceptions.CustomerNotFoundException;
import minierp.model.customer.Customer;

import java.sql.SQLException;
import java.util.List;

public interface CustomerDAO {

    //CRUD-Methoden:

    //create
    void createCustomer(Customer customer) throws CustomerAlreadyExistsException, SQLException;

    //read
    Customer findCustomerByID(int customerID) throws CustomerNotFoundException, SQLException;
    Customer findCustomerByEmail(String email) throws CustomerNotFoundException, SQLException;
    List<Customer> findCustomerByNameContaining(String namePattern) throws SQLException;
    List<Customer> findAllActiveCustomers() throws SQLException;
    List<Customer> findAllCustomers() throws SQLException;

    //update
    void updateCustomer(Customer customer) throws CustomerNotFoundException, SQLException;

    //delete (soft-delete)
    void deactivateCustomer(int customerID) throws CustomerNotFoundException, SQLException; //setActive(false)

}
