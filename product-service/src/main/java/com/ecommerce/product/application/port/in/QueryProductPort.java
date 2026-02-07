package com.ecommerce.product.application.port.in;

import com.ecommerce.product.application.dto.ProductResult;
import java.util.Optional;

public interface QueryProductPort {
    Optional<ProductResult> queryProduct(String productId);
}
