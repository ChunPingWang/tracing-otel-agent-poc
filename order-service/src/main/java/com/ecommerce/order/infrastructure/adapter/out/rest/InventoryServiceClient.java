package com.ecommerce.order.infrastructure.adapter.out.rest;

import com.ecommerce.order.application.port.out.InventoryReleasePort;
import com.ecommerce.order.application.port.out.InventoryReservePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryServiceClient implements InventoryReservePort, InventoryReleasePort {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(RestTemplate restTemplate,
                                  @Value("${inventory-service.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @Override
    public boolean reserveInventory(String productId, int quantity) {
        String url = inventoryServiceUrl + "/api/inventory/reserve";
        Map<String, Object> request = new HashMap<>();
        request.put("productId", productId);
        request.put("quantity", quantity);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
        return Boolean.TRUE.equals(response.get("reserved"));
    }

    @Override
    public void releaseInventory(String productId, int quantity) {
        String url = inventoryServiceUrl + "/api/inventory/release";
        Map<String, Object> request = new HashMap<>();
        request.put("productId", productId);
        request.put("quantity", quantity);

        restTemplate.postForObject(url, request, Map.class);
    }
}
