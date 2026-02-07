package com.ecommerce.payment.application.service;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;
import com.ecommerce.payment.application.port.in.ProcessPaymentPort;
import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    private ProcessPaymentPort processPaymentPort;

    @BeforeEach
    void setUp() {
        processPaymentPort = new ProcessPaymentUseCase(paymentRepository);
    }

    @Test
    void should_process_payment_successfully() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return Payment.reconstitute(1L, p.getPaymentId(), p.getOrderId(),
                    p.getAmount(), p.getStatus(), p.getCreatedAt());
        });

        PaymentResult result = processPaymentPort.processPayment(
                new PaymentCommand("ORD-001", new BigDecimal("1990.00")));

        assertNotNull(result.getPaymentId());
        assertEquals("SUCCESS", result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void should_save_payment_with_correct_data() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return Payment.reconstitute(1L, p.getPaymentId(), p.getOrderId(),
                    p.getAmount(), p.getStatus(), p.getCreatedAt());
        });

        processPaymentPort.processPayment(
                new PaymentCommand("ORD-002", new BigDecimal("500.00")));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertEquals("ORD-002", saved.getOrderId());
        assertEquals(0, new BigDecimal("500.00").compareTo(saved.getAmount()));
        assertEquals(PaymentStatus.SUCCESS, saved.getStatus());
    }
}
