package com.ecommerce.product.infrastructure.adapter.out.persistence;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.port.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaProductRepositoryAdapter implements ProductRepository {

    private final SpringDataProductRepository springDataRepo;

    public JpaProductRepositoryAdapter(SpringDataProductRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<Product> findByProductId(String productId) {
        return springDataRepo.findByProductId(productId).map(this::toDomain);
    }

    private Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.getId(),
                entity.getProductId(),
                entity.getName(),
                entity.getPrice(),
                entity.isAvailable()
        );
    }
}
