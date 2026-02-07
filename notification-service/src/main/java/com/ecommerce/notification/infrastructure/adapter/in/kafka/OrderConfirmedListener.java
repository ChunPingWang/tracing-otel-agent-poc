package com.ecommerce.notification.infrastructure.adapter.in.kafka;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;
import com.ecommerce.notification.application.port.in.ProcessOrderNotificationPort;
import com.ecommerce.notification.infrastructure.config.FailureSimulatorConfig;
import com.ecommerce.notification.infrastructure.dto.OrderConfirmedMessage;
import com.ecommerce.notification.infrastructure.mapper.NotificationInfraMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedListener.class);

    private final ProcessOrderNotificationPort notificationPort;
    private final FailureSimulatorConfig failureSimulatorConfig;

    public OrderConfirmedListener(ProcessOrderNotificationPort notificationPort,
                                  FailureSimulatorConfig failureSimulatorConfig) {
        this.notificationPort = notificationPort;
        this.failureSimulatorConfig = failureSimulatorConfig;
    }

    @KafkaListener(topics = "order-confirmed", groupId = "notification-group")
    public void onOrderConfirmed(OrderConfirmedMessage message) {
        log.info("Received order-confirmed event for order: {}", message.getOrderId());
        if (failureSimulatorConfig.isFailureEnabled()) {
            throw new RuntimeException("Simulated notification processing failure");
        }
        OrderNotificationCommand command = NotificationInfraMapper.toCommand(message);
        notificationPort.processNotification(command);
    }
}
