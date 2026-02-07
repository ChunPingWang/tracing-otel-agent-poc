package com.ecommerce.inventory.domain.model;

/**
 * Domain model representing inventory for a product. Tracks available and reserved stock.
 */
public class Inventory {
    private Long id;
    private String productId;
    private int availableStock;
    private int reservedStock;

    public Inventory(Long id, String productId, int availableStock, int reservedStock) {
        if (availableStock < 0) {
            throw new IllegalArgumentException("Available stock cannot be negative");
        }
        if (reservedStock < 0) {
            throw new IllegalArgumentException("Reserved stock cannot be negative");
        }
        this.id = id;
        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
    }

    /** Reserves the given quantity by deducting from available stock. Throws if insufficient. */
    public void reserve(int quantity) {
        if (quantity > availableStock) {
            throw new InsufficientStockException(productId, quantity, availableStock);
        }
        this.availableStock -= quantity;
        this.reservedStock += quantity;
    }

    /** Releases the given quantity from reserved back to available stock. */
    public void release(int quantity) {
        if (quantity > reservedStock) {
            throw new IllegalStateException(
                String.format("Cannot release %d, only %d reserved", quantity, reservedStock));
        }
        this.reservedStock -= quantity;
        this.availableStock += quantity;
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public int getAvailableStock() { return availableStock; }
    public int getReservedStock() { return reservedStock; }
}
