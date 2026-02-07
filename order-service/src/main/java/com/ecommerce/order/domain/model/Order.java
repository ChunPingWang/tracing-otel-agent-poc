package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private Long id;
    private String orderId;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Order() {}

    public static Order create(String orderId, String customerId, List<OrderItem> items) {
        Order order = new Order();
        order.orderId = orderId;
        order.customerId = customerId;
        order.status = OrderStatus.CREATED;
        order.items = new ArrayList<>(items);
        order.totalAmount = order.calculateTotal();
        order.createdAt = LocalDateTime.now();
        order.updatedAt = LocalDateTime.now();
        return order;
    }

    public static Order reconstitute(Long id, String orderId, String customerId,
            OrderStatus status, BigDecimal totalAmount, List<OrderItem> items,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        Order order = new Order();
        order.id = id;
        order.orderId = orderId;
        order.customerId = customerId;
        order.status = status;
        order.totalAmount = totalAmount;
        order.items = new ArrayList<>(items);
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    public void confirm() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot confirm order in status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot fail order in status: " + status);
        }
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void paymentTimeout() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot timeout order in status: " + status);
        }
        this.status = OrderStatus.PAYMENT_TIMEOUT;
        this.updatedAt = LocalDateTime.now();
    }

    private BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
