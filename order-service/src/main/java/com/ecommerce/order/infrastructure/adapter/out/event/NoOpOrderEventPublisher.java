package com.ecommerce.order.infrastructure.adapter.out.event;

import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpOrderEventPublisher implements OrderEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpOrderEventPublisher.class);

    @Override
    public void publish(OrderConfirmedEvent event) {
        log.info("NoOp: OrderConfirmedEvent for orderId={} (Kafka not configured yet)",
                event.getOrderId());
    }
}
