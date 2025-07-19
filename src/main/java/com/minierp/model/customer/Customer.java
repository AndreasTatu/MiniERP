package com.minierp.model.customer;

import java.time.LocalDate;
import java.util.Objects;

public class Customer {

    private int customerID;
    private String name;
    private String address;
    private LocalDate birthdate; //nullable
    private String email;
    private String phone;
    private boolean active = true;

    public Customer(String name, String address, LocalDate birthdate, String email, String phone) {
        this.name = name;
        this.address = address;
        this.birthdate = birthdate;
        this.email = email;
        this.phone = phone;
    }

    public Customer(int customerID, String name, String address, LocalDate birthdate, String email, String phone, boolean active) {
        this.customerID = customerID;
        this.name = name;
        this.address = address;
        this.birthdate = birthdate;
        this.email = email;
        this.phone = phone;
        this.active = active;
    }

    public Customer() {
        // for Tests, Setter, Mapping
    }

    public int getCustomerID() {
        return customerID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerID=" + customerID +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", birthdate=" + birthdate +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(email, customer.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }

}
