package com.ecommerce.payment.infrastructure.adapter.in.rest;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;
import com.ecommerce.payment.application.port.in.ProcessPaymentPort;
import com.ecommerce.payment.infrastructure.config.DelaySimulatorConfig;
import com.ecommerce.payment.infrastructure.dto.PaymentRequest;
import com.ecommerce.payment.infrastructure.dto.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment processing. Exposes POST /api/payments.
 * Supports configurable delay simulation for timeout testing.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentPort processPaymentPort;
    private final DelaySimulatorConfig delayConfig;

    public PaymentController(ProcessPaymentPort processPaymentPort,
                             DelaySimulatorConfig delayConfig) {
        this.processPaymentPort = processPaymentPort;
        this.delayConfig = delayConfig;
    }

    /** Processes a payment, applying any configured delay before execution. */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        delayConfig.applyDelay();
        PaymentResult result = processPaymentPort.processPayment(
                new PaymentCommand(request.getOrderId(), request.getAmount()));
        return ResponseEntity.ok(new PaymentResponse(result.getPaymentId(), result.getStatus()));
    }
}
