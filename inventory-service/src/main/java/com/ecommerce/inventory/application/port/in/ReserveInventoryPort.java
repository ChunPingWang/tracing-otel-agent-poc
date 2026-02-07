package com.ecommerce.inventory.application.port.in;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;

/**
 * Inbound port for reserving inventory stock for a given product.
 */
public interface ReserveInventoryPort {

    /**
     * Reserves the requested quantity from available stock.
     *
     * @param command the reserve command containing product ID and quantity
     * @return the result indicating success and remaining stock
     */
    ReserveResult reserve(ReserveCommand command);
}
