package com.ecommerce.product.application.port.in;

import com.ecommerce.product.application.dto.ProductResult;
import java.util.Optional;

/**
 * Inbound port for querying product information.
 */
public interface QueryProductPort {

    /**
     * Queries a product by its product ID.
     *
     * @param productId the unique product identifier
     * @return product details if found, empty otherwise
     */
    Optional<ProductResult> queryProduct(String productId);
}
