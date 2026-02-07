package com.ecommerce.product.infrastructure.mapper;

import com.ecommerce.product.application.dto.ProductResult;
import com.ecommerce.product.infrastructure.dto.ProductResponse;

public class ProductInfraMapper {
    public static ProductResponse toResponse(ProductResult result) {
        return new ProductResponse(
                result.getProductId(),
                result.getName(),
                result.getPrice(),
                result.isAvailable()
        );
    }
}
