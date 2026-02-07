package com.ecommerce.payment.infrastructure.mapper;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.infrastructure.dto.PaymentRequest;

public class PaymentInfraMapper {
    public static PaymentCommand toCommand(PaymentRequest request) {
        return new PaymentCommand(request.getOrderId(), request.getAmount());
    }
}
