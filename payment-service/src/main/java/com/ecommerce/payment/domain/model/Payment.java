package com.ecommerce.payment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private Long id;
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    private Payment() {}

    public static Payment createSuccess(String paymentId, String orderId, BigDecimal amount) {
        return create(paymentId, orderId, amount, PaymentStatus.SUCCESS);
    }

    public static Payment createFailed(String paymentId, String orderId, BigDecimal amount) {
        return create(paymentId, orderId, amount, PaymentStatus.FAILED);
    }

    private static Payment create(String paymentId, String orderId, BigDecimal amount, PaymentStatus status) {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        Payment payment = new Payment();
        payment.paymentId = paymentId;
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = status;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }

    public static Payment reconstitute(Long id, String paymentId, String orderId,
            BigDecimal amount, PaymentStatus status, LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.paymentId = paymentId;
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = status;
        payment.createdAt = createdAt;
        return payment;
    }

    public Long getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
