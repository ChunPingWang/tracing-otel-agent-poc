package com.ecommerce.inventory.domain.port;

import com.ecommerce.inventory.domain.model.Inventory;
import java.util.Optional;

/**
 * Domain port for persisting and retrieving Inventory entities.
 */
public interface InventoryRepository {

    /** Finds inventory by product ID. */
    Optional<Inventory> findByProductId(String productId);

    /** Saves an inventory entity and returns the persisted instance. */
    Inventory save(Inventory inventory);
}
