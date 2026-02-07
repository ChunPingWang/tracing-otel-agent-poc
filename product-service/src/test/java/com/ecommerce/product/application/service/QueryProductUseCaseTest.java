package com.ecommerce.product.application.service;

import com.ecommerce.product.application.dto.ProductResult;
import com.ecommerce.product.application.port.in.QueryProductPort;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    private QueryProductPort queryProductPort;

    @BeforeEach
    void setUp() {
        queryProductPort = new QueryProductUseCase(productRepository);
    }

    @Test
    void should_return_product_info_when_product_exists() {
        Product product = new Product(1L, "P001", "無線藍牙耳機", new BigDecimal("995.00"), true);
        when(productRepository.findByProductId("P001")).thenReturn(Optional.of(product));

        Optional<ProductResult> result = queryProductPort.queryProduct("P001");

        assertTrue(result.isPresent());
        assertEquals("P001", result.get().getProductId());
        assertEquals("無線藍牙耳機", result.get().getName());
        assertEquals(new BigDecimal("995.00"), result.get().getPrice());
        assertTrue(result.get().isAvailable());
    }

    @Test
    void should_return_empty_when_product_not_found() {
        when(productRepository.findByProductId("P999")).thenReturn(Optional.empty());

        Optional<ProductResult> result = queryProductPort.queryProduct("P999");

        assertFalse(result.isPresent());
    }
}
