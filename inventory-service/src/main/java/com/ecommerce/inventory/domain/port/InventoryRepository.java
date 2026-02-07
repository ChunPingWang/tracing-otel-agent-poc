package com.ecommerce.inventory.domain.port;

import com.ecommerce.inventory.domain.model.Inventory;
import java.util.Optional;

public interface InventoryRepository {
    Optional<Inventory> findByProductId(String productId);
    Inventory save(Inventory inventory);
}
