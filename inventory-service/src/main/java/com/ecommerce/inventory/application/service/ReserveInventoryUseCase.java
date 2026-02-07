package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;
import com.ecommerce.inventory.application.port.in.ReserveInventoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.port.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveInventoryUseCase implements ReserveInventoryPort {

    private final InventoryRepository inventoryRepository;

    public ReserveInventoryUseCase(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public ReserveResult reserve(ReserveCommand command) {
        Inventory inventory = inventoryRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + command.getProductId()));

        inventory.reserve(command.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);

        return new ReserveResult(true, saved.getAvailableStock());
    }
}
