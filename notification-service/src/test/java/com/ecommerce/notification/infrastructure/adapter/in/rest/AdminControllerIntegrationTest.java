package com.ecommerce.notification.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-confirmed", "order-confirmed.DLT"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_enable_failure_simulation() throws Exception {
        mockMvc.perform(post("/api/admin/simulate-failure?enabled=true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Failure simulation: enabled"));
    }

    @Test
    void should_disable_failure_simulation() throws Exception {
        mockMvc.perform(post("/api/admin/simulate-failure?enabled=false"))
                .andExpect(status().isOk())
                .andExpect(content().string("Failure simulation: disabled"));
    }
}
