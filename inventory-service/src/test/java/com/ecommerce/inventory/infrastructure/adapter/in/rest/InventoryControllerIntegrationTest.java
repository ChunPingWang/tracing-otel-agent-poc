package com.ecommerce.inventory.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
    "DELETE FROM inventory",
    "INSERT INTO inventory (product_id, available_stock, reserved_stock) VALUES ('P001', 50, 0)",
    "INSERT INTO inventory (product_id, available_stock, reserved_stock) VALUES ('P002', 100, 0)",
    "INSERT INTO inventory (product_id, available_stock, reserved_stock) VALUES ('P003', 200, 0)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_reserve_inventory_successfully() throws Exception {
        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"P001\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(true))
                .andExpect(jsonPath("$.remainingStock").value(48));
    }

    @Test
    void should_return_409_when_insufficient_stock() throws Exception {
        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"P003\",\"quantity\":999}"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_release_inventory_successfully() throws Exception {
        // First reserve
        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"P002\",\"quantity\":5}"));

        // Then release
        mockMvc.perform(post("/api/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"P002\",\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.released").value(true));
    }
}
