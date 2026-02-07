package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.event.OrderConfirmedEvent;

/**
 * Outbound port for publishing order events to Kafka.
 */
public interface OrderEventPublisherPort {

    /**
     * Publishes an order-confirmed event to the Kafka topic.
     *
     * @param event the order confirmed event to publish
     */
    void publish(OrderConfirmedEvent event);
}
