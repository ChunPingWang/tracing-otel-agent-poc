package com.ecommerce.order.application.dto;

import java.math.BigDecimal;

public class CreateOrderResult {
    private final String orderId;
    private final String status;
    private final BigDecimal totalAmount;

    public CreateOrderResult(String orderId, String status, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
