package com.ecommerce.order.infrastructure.dto;

import java.math.BigDecimal;

public class OrderItemMessage {
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItemMessage() {}

    public OrderItemMessage(String productId, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
