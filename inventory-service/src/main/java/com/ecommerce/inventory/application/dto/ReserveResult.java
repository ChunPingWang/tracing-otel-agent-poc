package com.ecommerce.inventory.application.dto;

public class ReserveResult {
    private final boolean reserved;
    private final int remainingStock;

    public ReserveResult(boolean reserved, int remainingStock) {
        this.reserved = reserved;
        this.remainingStock = remainingStock;
    }

    public boolean isReserved() { return reserved; }
    public int getRemainingStock() { return remainingStock; }
}
