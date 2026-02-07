package com.ecommerce.order.application.port.out;

public interface ProductQueryPort {
    ProductInfo queryProduct(String productId);
}
