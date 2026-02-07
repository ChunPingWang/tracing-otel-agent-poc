package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.dto.ReleaseCommand;
import com.ecommerce.inventory.application.port.in.ReleaseInventoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.port.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for releasing previously reserved inventory back to available stock.
 */
@Service
public class ReleaseInventoryUseCase implements ReleaseInventoryPort {

    private final InventoryRepository inventoryRepository;

    public ReleaseInventoryUseCase(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void release(ReleaseCommand command) {
        Inventory inventory = inventoryRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + command.getProductId()));

        inventory.release(command.getQuantity());
        inventoryRepository.save(inventory);
    }
}
