package com.ecommerce.order.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    @Test
    void should_create_order_with_status_created() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 2, new BigDecimal("995.00"))));
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals("ORD-001", order.getOrderId());
        assertEquals("C001", order.getCustomerId());
        assertEquals(new BigDecimal("1990.00"), order.getTotalAmount());
    }

    @Test
    void should_transition_to_confirmed() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 1, new BigDecimal("995.00"))));
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void should_transition_to_failed() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 1, new BigDecimal("995.00"))));
        order.fail();
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void should_transition_to_payment_timeout() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 1, new BigDecimal("995.00"))));
        order.paymentTimeout();
        assertEquals(OrderStatus.PAYMENT_TIMEOUT, order.getStatus());
    }

    @Test
    void should_not_confirm_already_failed_order() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(new OrderItem("P001", 1, new BigDecimal("995.00"))));
        order.fail();
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    void should_calculate_total_amount_from_items() {
        Order order = Order.create("ORD-001", "C001",
                Arrays.asList(
                    new OrderItem("P001", 2, new BigDecimal("995.00")),
                    new OrderItem("P002", 1, new BigDecimal("299.00"))
                ));
        assertEquals(new BigDecimal("2289.00"), order.getTotalAmount());
    }
}
