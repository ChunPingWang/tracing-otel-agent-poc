package com.ecommerce.notification.application.port.out;

public interface NotificationSenderPort {
    void send(String email, String message);
}
