package com.ecommerce.order.domain.port;

import com.ecommerce.order.domain.model.Order;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByOrderId(String orderId);
}
