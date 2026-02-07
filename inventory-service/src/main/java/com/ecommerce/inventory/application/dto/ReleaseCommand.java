package com.ecommerce.inventory.application.dto;

public class ReleaseCommand {
    private final String productId;
    private final int quantity;

    public ReleaseCommand(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
