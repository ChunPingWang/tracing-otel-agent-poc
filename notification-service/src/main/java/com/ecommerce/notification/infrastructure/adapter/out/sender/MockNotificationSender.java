package com.ecommerce.notification.infrastructure.adapter.out.sender;

import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationSender.class);

    @Override
    public void send(String email, String message) {
        log.info("[MOCK] Sending notification to {}: {}", email, message);
    }
}
