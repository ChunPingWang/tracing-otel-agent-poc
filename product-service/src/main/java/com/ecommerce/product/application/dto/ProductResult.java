package com.ecommerce.product.application.dto;

import java.math.BigDecimal;

public class ProductResult {
    private final String productId;
    private final String name;
    private final BigDecimal price;
    private final boolean available;

    public ProductResult(String productId, String name, BigDecimal price, boolean available) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
