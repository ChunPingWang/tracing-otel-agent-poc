package com.ecommerce.order.application.port.out;

import java.math.BigDecimal;

public interface PaymentPort {
    PaymentResult processPayment(String orderId, BigDecimal amount);
}
