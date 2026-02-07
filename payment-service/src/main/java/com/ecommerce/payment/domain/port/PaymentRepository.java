package com.ecommerce.payment.domain.port;

import com.ecommerce.payment.domain.model.Payment;
import java.util.Optional;

/**
 * Domain port for persisting and retrieving Payment entities.
 */
public interface PaymentRepository {

    /** Saves a payment and returns the persisted instance. */
    Payment save(Payment payment);

    /** Finds a payment by its business payment ID. */
    Optional<Payment> findByPaymentId(String paymentId);
}
