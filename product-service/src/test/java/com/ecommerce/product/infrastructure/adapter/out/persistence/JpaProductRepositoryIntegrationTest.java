package com.ecommerce.product.infrastructure.adapter.out.persistence;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.port.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaProductRepositoryAdapter.class)
@Sql(statements = {
    "INSERT INTO products (product_id, name, price, available) VALUES ('P001', '無線藍牙耳機', 995.00, true)",
    "INSERT INTO products (product_id, name, price, available) VALUES ('P002', 'USB-C 充電線', 299.00, true)"
})
public class JpaProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void should_find_product_by_product_id() {
        Optional<Product> product = productRepository.findByProductId("P001");
        assertTrue(product.isPresent());
        assertEquals("無線藍牙耳機", product.get().getName());
        assertEquals(0, new BigDecimal("995.00").compareTo(product.get().getPrice()));
        assertTrue(product.get().isAvailable());
    }

    @Test
    void should_return_empty_for_nonexistent_product() {
        Optional<Product> product = productRepository.findByProductId("P999");
        assertFalse(product.isPresent());
    }
}
