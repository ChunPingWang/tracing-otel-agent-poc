package com.ecommerce.notification.infrastructure.mapper;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;
import com.ecommerce.notification.infrastructure.dto.OrderConfirmedMessage;

public final class NotificationInfraMapper {

    private NotificationInfraMapper() {
    }

    public static OrderNotificationCommand toCommand(OrderConfirmedMessage message) {
        return new OrderNotificationCommand(
                message.getOrderId(),
                message.getCustomerId(),
                message.getTotalAmount()
        );
    }
}
