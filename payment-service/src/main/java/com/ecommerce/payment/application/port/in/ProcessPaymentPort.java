package com.ecommerce.payment.application.port.in;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;

public interface ProcessPaymentPort {
    PaymentResult processPayment(PaymentCommand command);
}
