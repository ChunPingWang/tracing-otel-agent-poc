package com.ecommerce.payment.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_process_payment_and_return_success() throws Exception {
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD-001\",\"amount\":1990.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void should_set_and_clear_delay_simulation() throws Exception {
        // Set delay to 5000ms
        mockMvc.perform(post("/api/admin/simulate-delay?ms=5000"))
                .andExpect(status().isOk());

        // Clear delay
        mockMvc.perform(post("/api/admin/simulate-delay?ms=0"))
                .andExpect(status().isOk());
    }
}
