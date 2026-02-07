package com.ecommerce.order.application.port.in;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;

/**
 * Inbound port for creating orders. Entry point for the order creation use case.
 */
public interface CreateOrderPort {

    /**
     * Creates an order by orchestrating product query, inventory reserve, payment, and notification.
     *
     * @param command the order creation command containing customer and item info
     * @return the result containing order ID, status, and total amount
     */
    CreateOrderResult createOrder(CreateOrderCommand command);
}
