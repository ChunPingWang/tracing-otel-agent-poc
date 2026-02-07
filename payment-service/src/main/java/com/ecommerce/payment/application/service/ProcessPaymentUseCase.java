package com.ecommerce.payment.application.service;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;
import com.ecommerce.payment.application.port.in.ProcessPaymentPort;
import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessPaymentUseCase implements ProcessPaymentPort {

    private final PaymentRepository paymentRepository;

    public ProcessPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResult processPayment(PaymentCommand command) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = Payment.createSuccess(paymentId, command.getOrderId(), command.getAmount());
        Payment saved = paymentRepository.save(payment);
        return new PaymentResult(saved.getPaymentId(), saved.getStatus().name());
    }
}
