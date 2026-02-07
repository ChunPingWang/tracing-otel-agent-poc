package com.ecommerce.inventory.domain.model;

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

    public void reserve(int quantity) {
        if (quantity > availableStock) {
            throw new InsufficientStockException(productId, quantity, availableStock);
        }
        this.availableStock -= quantity;
        this.reservedStock += quantity;
    }

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
