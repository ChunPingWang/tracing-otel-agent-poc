package com.ecommerce.product.infrastructure.adapter.in.rest;

import com.ecommerce.product.application.port.in.QueryProductPort;
import com.ecommerce.product.infrastructure.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final QueryProductPort queryProductPort;

    public ProductController(QueryProductPort queryProductPort) {
        this.queryProductPort = queryProductPort;
    }

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
