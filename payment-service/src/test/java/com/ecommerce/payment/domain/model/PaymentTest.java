package com.ecommerce.payment.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentTest {

    @Test
    void should_create_successful_payment() {
        Payment payment = Payment.createSuccess("PAY-001", "ORD-001", new BigDecimal("1990.00"));
        assertEquals("PAY-001", payment.getPaymentId());
        assertEquals("ORD-001", payment.getOrderId());
        assertEquals(new BigDecimal("1990.00"), payment.getAmount());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNotNull(payment.getCreatedAt());
    }

    @Test
    void should_create_failed_payment() {
        Payment payment = Payment.createFailed("PAY-002", "ORD-002", new BigDecimal("500.00"));
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void should_not_allow_negative_amount() {
        assertThrows(IllegalArgumentException.class,
            () -> Payment.createSuccess("PAY-001", "ORD-001", new BigDecimal("-100.00")));
    }

    @Test
    void should_not_allow_null_order_id() {
        assertThrows(IllegalArgumentException.class,
            () -> Payment.createSuccess("PAY-001", null, new BigDecimal("100.00")));
    }
}
