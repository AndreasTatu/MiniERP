package dao.api.customer;

import common.exceptions.CustomerNotFoundException;
import model.customer.Customer;

import java.util.List;

public interface CustomerDAO {

    //CRUD-Methoden:

    //create
    void createCustomer(Customer customer);

    //read
    Customer findCustomerByID(int customerID) throws CustomerNotFoundException;
    Customer findCustomerByEmail(String email) throws CustomerNotFoundException;
    List<Customer> getAllActiveCustomers();

    //update
    void updateCustomer(Customer customer) throws CustomerNotFoundException;

    //delete (soft-delete)
    void deactivateCustomer(int customerID) throws CustomerNotFoundException; //setActive(false)

}
