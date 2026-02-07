package com.ecommerce.order.domain.model;

/**
 * Represents the lifecycle states of an order: CREATED, CONFIRMED, FAILED, PAYMENT_TIMEOUT.
 */
public enum OrderStatus {
    CREATED, CONFIRMED, FAILED, PAYMENT_TIMEOUT
}
