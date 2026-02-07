package com.ecommerce.payment.infrastructure.adapter.out.persistence;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaPaymentRepositoryAdapter.class)
public class JpaPaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void should_save_and_find_payment() {
        Payment payment = Payment.createSuccess("PAY-001", "ORD-001", new BigDecimal("1990.00"));
        Payment saved = paymentRepository.save(payment);
        assertNotNull(saved.getId());

        Optional<Payment> found = paymentRepository.findByPaymentId("PAY-001");
        assertTrue(found.isPresent());
        assertEquals("PAY-001", found.get().getPaymentId());
        assertEquals("ORD-001", found.get().getOrderId());
        assertEquals(PaymentStatus.SUCCESS, found.get().getStatus());
    }

    @Test
    void should_return_empty_for_nonexistent() {
        Optional<Payment> found = paymentRepository.findByPaymentId("NONEXISTENT");
        assertFalse(found.isPresent());
    }
}
