package com.ecommerce.order.domain.model;

import java.math.BigDecimal;

/**
 * Value object representing a line item in an order, containing product, quantity, and unit price.
 */
public class OrderItem {
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem(String productId, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** Calculates the subtotal as unitPrice * quantity. */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
