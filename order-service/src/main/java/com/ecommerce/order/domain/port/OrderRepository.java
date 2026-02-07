package com.ecommerce.order.domain.port;

import com.ecommerce.order.domain.model.Order;
import java.util.Optional;

/**
 * Domain port for persisting and retrieving Order aggregates.
 */
public interface OrderRepository {

    /** Saves an order and returns the persisted instance. */
    Order save(Order order);

    /** Finds an order by its business order ID. */
    Optional<Order> findByOrderId(String orderId);
}
