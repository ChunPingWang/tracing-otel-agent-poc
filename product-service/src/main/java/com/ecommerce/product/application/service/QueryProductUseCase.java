package com.ecommerce.product.application.service;

import com.ecommerce.product.application.dto.ProductResult;
import com.ecommerce.product.application.port.in.QueryProductPort;
import com.ecommerce.product.domain.port.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QueryProductUseCase implements QueryProductPort {

    private final ProductRepository productRepository;

    public QueryProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductResult> queryProduct(String productId) {
        return productRepository.findByProductId(productId)
                .map(product -> new ProductResult(
                        product.getProductId(),
                        product.getName(),
                        product.getPrice(),
                        product.isAvailable()
                ));
    }
}
