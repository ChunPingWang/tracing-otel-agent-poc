package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;
import com.ecommerce.inventory.application.port.in.ReserveInventoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InsufficientStockException;
import com.ecommerce.inventory.domain.port.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReserveInventoryUseCaseTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private ReserveInventoryPort reserveInventoryPort;

    @BeforeEach
    void setUp() {
        reserveInventoryPort = new ReserveInventoryUseCase(inventoryRepository);
    }

    @Test
    void should_reserve_stock_successfully() {
        Inventory inventory = new Inventory(1L, "P001", 50, 0);
        when(inventoryRepository.findByProductId("P001")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(new Inventory(1L, "P001", 48, 2));

        ReserveResult result = reserveInventoryPort.reserve(new ReserveCommand("P001", 2));

        assertTrue(result.isReserved());
        assertEquals(48, result.getRemainingStock());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void should_throw_when_insufficient_stock() {
        Inventory inventory = new Inventory(1L, "P001", 1, 0);
        when(inventoryRepository.findByProductId("P001")).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
            () -> reserveInventoryPort.reserve(new ReserveCommand("P001", 10)));
    }

    @Test
    void should_throw_when_product_not_found() {
        when(inventoryRepository.findByProductId("P999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> reserveInventoryPort.reserve(new ReserveCommand("P999", 1)));
    }
}
