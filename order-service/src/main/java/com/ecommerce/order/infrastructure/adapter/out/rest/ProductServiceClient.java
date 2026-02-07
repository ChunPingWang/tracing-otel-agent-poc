package com.ecommerce.order.infrastructure.adapter.out.rest;

import com.ecommerce.order.application.port.out.ProductInfo;
import com.ecommerce.order.application.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ProductServiceClient implements ProductQueryPort {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductServiceClient(RestTemplate restTemplate,
                                @Value("${product-service.url}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public ProductInfo queryProduct(String productId) {
        String url = productServiceUrl + "/api/products/{productId}";
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class, productId);
        return mapToProductInfo(response);
    }

    private ProductInfo mapToProductInfo(Map<String, Object> response) {
        String productId = (String) response.get("productId");
        String name = (String) response.get("name");
        BigDecimal price = new BigDecimal(response.get("price").toString());
        return new ProductInfo(productId, name, price);
    }
}
