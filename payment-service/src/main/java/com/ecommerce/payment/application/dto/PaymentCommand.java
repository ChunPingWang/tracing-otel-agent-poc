package com.ecommerce.payment.application.dto;

import java.math.BigDecimal;

public class PaymentCommand {
    private final String orderId;
    private final BigDecimal amount;

    public PaymentCommand(String orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
}
