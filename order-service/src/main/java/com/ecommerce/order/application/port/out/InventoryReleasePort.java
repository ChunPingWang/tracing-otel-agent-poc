package com.ecommerce.order.application.port.out;

public interface InventoryReleasePort {
    void releaseInventory(String productId, int quantity);
}
