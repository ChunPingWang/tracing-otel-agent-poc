package com.ecommerce.inventory.infrastructure.dto;

public class ReserveRequest {
    private String productId;
    private int quantity;

    public ReserveRequest() {}
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
