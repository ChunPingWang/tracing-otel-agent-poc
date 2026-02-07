package com.ecommerce.product.infrastructure.adapter.in.rest;

import com.ecommerce.product.application.port.in.QueryProductPort;
import com.ecommerce.product.infrastructure.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for product queries. Exposes GET /api/products/{productId}.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final QueryProductPort queryProductPort;

    public ProductController(QueryProductPort queryProductPort) {
        this.queryProductPort = queryProductPort;
    }

    /** Returns product details by product ID, or 404 if not found. */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
        return queryProductPort.queryProduct(productId)
                .map(result -> ResponseEntity.ok(new ProductResponse(
                        result.getProductId(),
                        result.getName(),
                        result.getPrice(),
                        result.isAvailable()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
