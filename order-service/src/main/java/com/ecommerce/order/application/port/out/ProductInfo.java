package com.ecommerce.order.application.port.out;

import java.math.BigDecimal;

public class ProductInfo {
    private final String productId;
    private final String name;
    private final BigDecimal price;

    public ProductInfo(String productId, String name, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
