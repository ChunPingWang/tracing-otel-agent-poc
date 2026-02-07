package com.ecommerce.notification.application.port.out;

/**
 * Outbound port for sending notifications (e.g. email). Simulated in this PoC.
 */
public interface NotificationSenderPort {

    /**
     * Sends a notification message to the specified email address.
     *
     * @param email   the recipient email address
     * @param message the notification message content
     */
    void send(String email, String message);
}
