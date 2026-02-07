package com.ecommerce.notification.application.port.in;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;

/**
 * Inbound port for processing order confirmation notifications consumed from Kafka.
 */
public interface ProcessOrderNotificationPort {

    /**
     * Processes a notification for an order confirmation event.
     *
     * @param command the notification command containing order and customer details
     */
    void processNotification(OrderNotificationCommand command);
}
