package com.ecommerce.order.application.port.out;

/**
 * Outbound port for reserving inventory via the Inventory Service HTTP API.
 */
public interface InventoryReservePort {

    /**
     * Reserves the specified quantity of a product in inventory.
     *
     * @param productId the product to reserve
     * @param quantity  the quantity to reserve
     * @return true if reservation succeeded
     */
    boolean reserveInventory(String productId, int quantity);
}
