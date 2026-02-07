package com.ecommerce.order.application.port.out;

/**
 * Outbound port for releasing previously reserved inventory via the Inventory Service HTTP API.
 */
public interface InventoryReleasePort {

    /**
     * Releases the specified quantity of reserved inventory back to available stock.
     *
     * @param productId the product to release
     * @param quantity  the quantity to release
     */
    void releaseInventory(String productId, int quantity);
}
