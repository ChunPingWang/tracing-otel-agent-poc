package com.ecommerce.order.infrastructure.adapter.out.kafka;

import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.infrastructure.dto.OrderConfirmedMessage;
import com.ecommerce.order.infrastructure.mapper.OrderInfraMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    private static final String TOPIC = "order-confirmed";

    private final KafkaTemplate<String, OrderConfirmedMessage> kafkaTemplate;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, OrderConfirmedMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(OrderConfirmedEvent event) {
        OrderConfirmedMessage message = OrderInfraMapper.toMessage(event);
        kafkaTemplate.send(TOPIC, event.getOrderId(), message);
        log.info("Published OrderConfirmedEvent to topic={} key={}",
                TOPIC, event.getOrderId());
    }
}
