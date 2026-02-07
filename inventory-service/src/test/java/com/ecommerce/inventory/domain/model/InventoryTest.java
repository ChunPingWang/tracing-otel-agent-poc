package com.ecommerce.inventory.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryTest {

    @Test
    void should_create_inventory() {
        Inventory inv = new Inventory(1L, "P001", 50, 0);
        assertEquals("P001", inv.getProductId());
        assertEquals(50, inv.getAvailableStock());
        assertEquals(0, inv.getReservedStock());
    }

    @Test
    void should_reserve_stock() {
        Inventory inv = new Inventory(1L, "P001", 50, 0);
        inv.reserve(10);
        assertEquals(40, inv.getAvailableStock());
        assertEquals(10, inv.getReservedStock());
    }

    @Test
    void should_release_stock() {
        Inventory inv = new Inventory(1L, "P001", 40, 10);
        inv.release(5);
        assertEquals(45, inv.getAvailableStock());
        assertEquals(5, inv.getReservedStock());
    }

    @Test
    void should_throw_when_insufficient_stock() {
        Inventory inv = new Inventory(1L, "P001", 5, 0);
        assertThrows(InsufficientStockException.class, () -> inv.reserve(10));
    }

    @Test
    void should_throw_when_release_exceeds_reserved() {
        Inventory inv = new Inventory(1L, "P001", 40, 10);
        assertThrows(IllegalStateException.class, () -> inv.release(15));
    }

    @Test
    void should_not_allow_negative_stock() {
        assertThrows(IllegalArgumentException.class,
            () -> new Inventory(1L, "P001", -1, 0));
    }
}
