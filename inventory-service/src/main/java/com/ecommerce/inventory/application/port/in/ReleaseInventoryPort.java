package com.ecommerce.inventory.application.port.in;

import com.ecommerce.inventory.application.dto.ReleaseCommand;

/**
 * Inbound port for releasing previously reserved inventory stock.
 */
public interface ReleaseInventoryPort {

    /**
     * Releases the reserved quantity back to available stock.
     *
     * @param command the release command containing product ID and quantity
     */
    void release(ReleaseCommand command);
}
