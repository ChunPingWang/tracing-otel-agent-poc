package com.ecommerce.order.application.dto;

public class OrderItemCommand {
    private final String productId;
    private final int quantity;

    public OrderItemCommand(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
