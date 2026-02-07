package com.ecommerce.order.infrastructure.adapter.in.rest;

import java.math.BigDecimal;

public class CreateOrderResponse {
    private String orderId;
    private String status;
    private BigDecimal totalAmount;
    private String traceId;

    public CreateOrderResponse() {}

    public CreateOrderResponse(String orderId, String status,
                               BigDecimal totalAmount, String traceId) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.traceId = traceId;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
