package com.ecommerce.order.infrastructure.adapter.out.rest;

import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentServiceClient implements PaymentPort {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate,
                                @Value("${payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    public PaymentResult processPayment(String orderId, BigDecimal amount) {
        String url = paymentServiceUrl + "/api/payments";
        Map<String, Object> request = new HashMap<>();
        request.put("orderId", orderId);
        request.put("amount", amount);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
        return mapToPaymentResult(response);
    }

    private PaymentResult mapToPaymentResult(Map<String, Object> response) {
        String paymentId = (String) response.get("paymentId");
        String status = (String) response.get("status");
        return new PaymentResult(paymentId, status);
    }
}
