package com.ecommerce.order.application.port.out;

import java.math.BigDecimal;

/**
 * Outbound port for processing payments via the Payment Service HTTP API.
 */
public interface PaymentPort {

    /**
     * Sends a payment request to the Payment Service.
     *
     * @param orderId the order ID to associate with the payment
     * @param amount  the payment amount
     * @return the payment result containing payment ID and status
     */
    PaymentResult processPayment(String orderId, BigDecimal amount);
}
