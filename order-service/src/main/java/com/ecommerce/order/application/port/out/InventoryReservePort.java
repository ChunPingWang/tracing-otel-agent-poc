package com.ecommerce.order.application.port.out;

public interface InventoryReservePort {
    boolean reserveInventory(String productId, int quantity);
}
