package com.ecommerce.payment.domain.port;

import com.ecommerce.payment.domain.model.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByPaymentId(String paymentId);
}
