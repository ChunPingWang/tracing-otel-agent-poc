package com.ecommerce.notification.domain.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private String orderId;
    private String customerId;
    private String customerEmail;
    private NotificationStatus status;
    private String message;
    private LocalDateTime createdAt;

    private Notification() {}

    public static Notification createSent(String orderId, String customerId,
            String customerEmail, String message) {
        return create(orderId, customerId, customerEmail, message, NotificationStatus.SENT);
    }

    public static Notification createFailed(String orderId, String customerId,
            String customerEmail, String message) {
        return create(orderId, customerId, customerEmail, message, NotificationStatus.FAILED);
    }

    private static Notification create(String orderId, String customerId,
            String customerEmail, String message, NotificationStatus status) {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        Notification n = new Notification();
        n.orderId = orderId;
        n.customerId = customerId;
        n.customerEmail = customerEmail;
        n.message = message;
        n.status = status;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public static Notification reconstitute(Long id, String orderId, String customerId,
            String customerEmail, NotificationStatus status, String message, LocalDateTime createdAt) {
        Notification n = new Notification();
        n.id = id;
        n.orderId = orderId;
        n.customerId = customerId;
        n.customerEmail = customerEmail;
        n.status = status;
        n.message = message;
        n.createdAt = createdAt;
        return n;
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public NotificationStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
