package com.ecommerce.product.application.mapper;

import com.ecommerce.product.application.dto.ProductResult;
import com.ecommerce.product.domain.model.Product;

public class ProductApplicationMapper {
    public static ProductResult toResult(Product product) {
        return new ProductResult(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.isAvailable()
        );
    }
}
