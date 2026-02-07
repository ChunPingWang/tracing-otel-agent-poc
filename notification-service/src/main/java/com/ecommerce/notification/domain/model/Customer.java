package com.ecommerce.notification.domain.model;

/**
 * Domain model representing a customer with contact information for notification delivery.
 */
public class Customer {
    private Long id;
    private String customerId;
    private String name;
    private String email;
    private String phone;

    public Customer(Long id, String customerId, String name, String email, String phone) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
