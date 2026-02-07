package com.ecommerce.product.domain.port;

import com.ecommerce.product.domain.model.Product;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findByProductId(String productId);
}
