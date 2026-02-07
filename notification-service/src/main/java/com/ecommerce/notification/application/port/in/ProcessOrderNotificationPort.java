package com.ecommerce.notification.application.port.in;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;

public interface ProcessOrderNotificationPort {
    void processNotification(OrderNotificationCommand command);
}
