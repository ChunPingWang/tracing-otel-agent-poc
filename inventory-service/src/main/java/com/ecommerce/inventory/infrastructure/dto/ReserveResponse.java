package com.ecommerce.inventory.infrastructure.dto;

public class ReserveResponse {
    private boolean reserved;
    private int remainingStock;

    public ReserveResponse() {}
    public ReserveResponse(boolean reserved, int remainingStock) {
        this.reserved = reserved;
        this.remainingStock = remainingStock;
    }
    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }
    public int getRemainingStock() { return remainingStock; }
    public void setRemainingStock(int remainingStock) { this.remainingStock = remainingStock; }
}
