package com.ecommerce.order.application.port.out;

/**
 * Outbound port for querying product information from the Product Service via HTTP.
 */
public interface ProductQueryPort {

    /**
     * Queries a product by its ID.
     *
     * @param productId the unique product identifier
     * @return product information including name and price
     */
    ProductInfo queryProduct(String productId);
}
