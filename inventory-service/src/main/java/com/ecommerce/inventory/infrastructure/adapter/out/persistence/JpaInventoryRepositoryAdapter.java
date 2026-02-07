package com.ecommerce.inventory.infrastructure.adapter.out.persistence;

import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.port.InventoryRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaInventoryRepositoryAdapter implements InventoryRepository {

    private final SpringDataInventoryRepository springDataRepo;

    public JpaInventoryRepositoryAdapter(SpringDataInventoryRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return springDataRepo.findByProductId(productId).map(this::toDomain);
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryJpaEntity entity = toEntity(inventory);
        InventoryJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    private InventoryJpaEntity toEntity(Inventory inv) {
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setId(inv.getId());
        entity.setProductId(inv.getProductId());
        entity.setAvailableStock(inv.getAvailableStock());
        entity.setReservedStock(inv.getReservedStock());
        return entity;
    }

    private Inventory toDomain(InventoryJpaEntity entity) {
        return new Inventory(
                entity.getId(),
                entity.getProductId(),
                entity.getAvailableStock(),
                entity.getReservedStock()
        );
    }
}
