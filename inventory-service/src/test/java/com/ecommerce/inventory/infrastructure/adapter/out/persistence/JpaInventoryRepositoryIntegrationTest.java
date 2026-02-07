package com.ecommerce.inventory.infrastructure.adapter.out.persistence;

import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.port.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaInventoryRepositoryAdapter.class)
@Sql(statements = {
    "INSERT INTO inventory (product_id, available_stock, reserved_stock) VALUES ('P001', 50, 0)",
    "INSERT INTO inventory (product_id, available_stock, reserved_stock) VALUES ('P002', 100, 0)"
})
public class JpaInventoryRepositoryIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void should_find_inventory_by_product_id() {
        Optional<Inventory> inv = inventoryRepository.findByProductId("P001");
        assertTrue(inv.isPresent());
        assertEquals(50, inv.get().getAvailableStock());
        assertEquals(0, inv.get().getReservedStock());
    }

    @Test
    void should_save_updated_inventory() {
        Inventory inv = inventoryRepository.findByProductId("P001").get();
        inv.reserve(10);
        inventoryRepository.save(inv);

        Inventory updated = inventoryRepository.findByProductId("P001").get();
        assertEquals(40, updated.getAvailableStock());
        assertEquals(10, updated.getReservedStock());
    }

    @Test
    void should_return_empty_for_nonexistent() {
        Optional<Inventory> inv = inventoryRepository.findByProductId("P999");
        assertFalse(inv.isPresent());
    }
}
