package com.ecommerce.order.application.port.out;

public class PaymentResult {
    private final String paymentId;
    private final String status;

    public PaymentResult(String paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    public String getPaymentId() { return paymentId; }
    public String getStatus() { return status; }

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
