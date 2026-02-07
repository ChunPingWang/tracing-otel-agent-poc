package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.event.OrderConfirmedEvent;

public interface OrderEventPublisherPort {
    void publish(OrderConfirmedEvent event);
}
