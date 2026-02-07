package com.ecommerce.order.infrastructure.adapter.in.rest;

import com.ecommerce.order.application.port.out.InventoryReservePort;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.application.port.out.ProductInfo;
import com.ecommerce.order.application.port.out.ProductQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductQueryPort productQueryPort;

    @MockBean
    private InventoryReservePort inventoryReservePort;

    @MockBean
    private PaymentPort paymentPort;

    @MockBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @Test
    void should_create_order_and_return_confirmed_status() throws Exception {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(inventoryReservePort.reserveInventory("P001", 2)).thenReturn(true);
        when(paymentPort.processPayment(anyString(), eq(new BigDecimal("1998.00"))))
                .thenReturn(new PaymentResult("PAY-001", "SUCCESS"));

        String requestJson = "{\"customerId\":\"C001\","
                + "\"items\":[{\"productId\":\"P001\",\"quantity\":2}]}";

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(1998.00))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void should_create_order_with_multiple_items() throws Exception {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(productQueryPort.queryProduct("P002"))
                .thenReturn(new ProductInfo("P002", "Mouse", new BigDecimal("29.00")));
        when(inventoryReservePort.reserveInventory(anyString(), anyInt())).thenReturn(true);
        when(paymentPort.processPayment(anyString(), eq(new BigDecimal("1028.00"))))
                .thenReturn(new PaymentResult("PAY-002", "SUCCESS"));

        String requestJson = "{\"customerId\":\"C001\","
                + "\"items\":["
                + "{\"productId\":\"P001\",\"quantity\":1},"
                + "{\"productId\":\"P002\",\"quantity\":1}"
                + "]}";

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(1028.00));
    }
}
