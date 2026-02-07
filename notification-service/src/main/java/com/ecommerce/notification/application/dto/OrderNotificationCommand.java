package com.ecommerce.notification.application.dto;

import java.math.BigDecimal;

public class OrderNotificationCommand {

    private final String orderId;
    private final String customerId;
    private final BigDecimal totalAmount;

    public OrderNotificationCommand(String orderId, String customerId, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
