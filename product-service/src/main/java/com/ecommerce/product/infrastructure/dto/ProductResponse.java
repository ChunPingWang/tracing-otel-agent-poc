package com.ecommerce.product.infrastructure.dto;

import java.math.BigDecimal;

public class ProductResponse {
    private String productId;
    private String name;
    private BigDecimal price;
    private boolean available;

    public ProductResponse() {}

    public ProductResponse(String productId, String name, BigDecimal price, boolean available) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
