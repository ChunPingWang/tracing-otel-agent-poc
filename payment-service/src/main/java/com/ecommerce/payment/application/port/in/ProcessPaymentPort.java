package com.ecommerce.payment.application.port.in;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;

/**
 * Inbound port for processing payments. Simulates payment processing for the PoC.
 */
public interface ProcessPaymentPort {

    /**
     * Processes a payment for the given order.
     *
     * @param command the payment command containing order ID and amount
     * @return the result containing payment ID and status
     */
    PaymentResult processPayment(PaymentCommand command);
}
