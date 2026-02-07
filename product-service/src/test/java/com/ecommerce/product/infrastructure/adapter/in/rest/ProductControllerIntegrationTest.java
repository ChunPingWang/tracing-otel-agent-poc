package com.ecommerce.product.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_product_when_exists() throws Exception {
        mockMvc.perform(get("/api/products/P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("P001"))
                .andExpect(jsonPath("$.name").value("無線藍牙耳機"))
                .andExpect(jsonPath("$.price").value(995.00))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void should_return_404_when_product_not_found() throws Exception {
        mockMvc.perform(get("/api/products/P999"))
                .andExpect(status().isNotFound());
    }
}
