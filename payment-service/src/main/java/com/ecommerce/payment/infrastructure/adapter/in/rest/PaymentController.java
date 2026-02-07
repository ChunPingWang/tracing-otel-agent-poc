package com.ecommerce.payment.infrastructure.adapter.in.rest;

import com.ecommerce.payment.application.dto.PaymentCommand;
import com.ecommerce.payment.application.dto.PaymentResult;
import com.ecommerce.payment.application.port.in.ProcessPaymentPort;
import com.ecommerce.payment.infrastructure.dto.PaymentRequest;
import com.ecommerce.payment.infrastructure.dto.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentPort processPaymentPort;

    public PaymentController(ProcessPaymentPort processPaymentPort) {
        this.processPaymentPort = processPaymentPort;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentResult result = processPaymentPort.processPayment(
                new PaymentCommand(request.getOrderId(), request.getAmount()));
        return ResponseEntity.ok(new PaymentResponse(result.getPaymentId(), result.getStatus()));
    }
}
