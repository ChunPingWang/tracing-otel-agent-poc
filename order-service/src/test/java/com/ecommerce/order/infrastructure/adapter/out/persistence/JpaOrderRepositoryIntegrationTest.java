package com.ecommerce.order.infrastructure.adapter.out.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaOrderRepositoryAdapter.class)
public class JpaOrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void should_save_and_find_order() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 2, new BigDecimal("995.00"))));

        Order saved = orderRepository.save(order);
        assertNotNull(saved.getId());

        Optional<Order> found = orderRepository.findByOrderId("ORD-001");
        assertTrue(found.isPresent());
        assertEquals("ORD-001", found.get().getOrderId());
        assertEquals(OrderStatus.CREATED, found.get().getStatus());
        assertEquals(new BigDecimal("1990.00"), found.get().getTotalAmount());
        assertEquals(1, found.get().getItems().size());
    }

    @Test
    void should_update_order_status() {
        Order order = Order.create("ORD-002", "C001",
                Arrays.asList(new OrderItem("P001", 1, new BigDecimal("995.00"))));
        orderRepository.save(order);

        Order toUpdate = orderRepository.findByOrderId("ORD-002").get();
        toUpdate.confirm();
        orderRepository.save(toUpdate);

        Order confirmed = orderRepository.findByOrderId("ORD-002").get();
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
    }

    @Test
    void should_return_empty_for_nonexistent_order() {
        Optional<Order> found = orderRepository.findByOrderId("NONEXISTENT");
        assertFalse(found.isPresent());
    }
}
