package com.ecommerce.inventory.domain.model;

/**
 * Domain exception thrown when a reserve request exceeds available inventory stock.
 */
public class InsufficientStockException extends RuntimeException {
    private final String productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, int requested, int available) {
        super(String.format("Insufficient stock for product %s: requested=%d, available=%d",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
