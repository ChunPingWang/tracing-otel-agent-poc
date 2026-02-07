package com.ecommerce.order.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event published when an order is confirmed. Sent to Kafka for async notification processing.
 */
public class OrderConfirmedEvent {
    private final String orderId;
    private final String customerId;
    private final List<OrderItemData> items;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;

    public OrderConfirmedEvent(String orderId, String customerId,
            List<OrderItemData> items, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItemData> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    /** Nested data class representing an order item within the event payload. */
    public static class OrderItemData {
        private final String productId;
        private final int quantity;
        private final BigDecimal unitPrice;

        public OrderItemData(String productId, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
    }
}
