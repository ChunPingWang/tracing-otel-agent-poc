package com.ecommerce.product.domain.model;

import java.math.BigDecimal;

/**
 * Domain model representing a product in the catalog with ID, name, price, and availability.
 */
public class Product {
    private Long id;
    private String productId;
    private String name;
    private BigDecimal price;
    private boolean available;

    public Product(Long id, String productId, String name, BigDecimal price, boolean available) {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
