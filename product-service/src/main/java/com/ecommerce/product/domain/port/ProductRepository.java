package com.ecommerce.product.domain.port;

import com.ecommerce.product.domain.model.Product;
import java.util.Optional;

/**
 * Domain port for retrieving Product entities from the data store.
 */
public interface ProductRepository {

    /** Finds a product by its business product ID. */
    Optional<Product> findByProductId(String productId);
}
