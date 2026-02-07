package com.ecommerce.product.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    @Test
    void should_create_product() {
        Product product = new Product(1L, "P001", "無線藍牙耳機", new BigDecimal("995.00"), true);
        assertEquals("P001", product.getProductId());
        assertEquals("無線藍牙耳機", product.getName());
        assertEquals(new BigDecimal("995.00"), product.getPrice());
        assertTrue(product.isAvailable());
    }

    @Test
    void should_create_unavailable_product() {
        Product product = new Product(2L, "P004", "已下架商品", new BigDecimal("100.00"), false);
        assertFalse(product.isAvailable());
    }

    @Test
    void should_not_allow_negative_price() {
        assertThrows(IllegalArgumentException.class,
            () -> new Product(1L, "P001", "Test", new BigDecimal("-1.00"), true));
    }

    @Test
    void should_not_allow_null_product_id() {
        assertThrows(IllegalArgumentException.class,
            () -> new Product(1L, null, "Test", new BigDecimal("100.00"), true));
    }
}
