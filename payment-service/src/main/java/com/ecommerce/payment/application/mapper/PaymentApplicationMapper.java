package com.ecommerce.payment.application.mapper;

import com.ecommerce.payment.application.dto.PaymentResult;
import com.ecommerce.payment.domain.model.Payment;

public class PaymentApplicationMapper {
    public static PaymentResult toResult(Payment payment) {
        return new PaymentResult(payment.getPaymentId(), payment.getStatus().name());
    }
}
