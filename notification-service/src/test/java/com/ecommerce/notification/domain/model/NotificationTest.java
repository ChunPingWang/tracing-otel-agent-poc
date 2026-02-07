package com.ecommerce.notification.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationTest {

    @Test
    void should_create_sent_notification() {
        Notification notification = Notification.createSent("ORD-001", "C001",
                "wang@example.com", "訂單 ORD-001 已確認");
        assertEquals("ORD-001", notification.getOrderId());
        assertEquals("C001", notification.getCustomerId());
        assertEquals("wang@example.com", notification.getCustomerEmail());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void should_create_failed_notification() {
        Notification notification = Notification.createFailed("ORD-002", "C002",
                "lee@example.com", "訂單 ORD-002 已確認");
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }

    @Test
    void should_not_allow_null_order_id() {
        assertThrows(IllegalArgumentException.class,
            () -> Notification.createSent(null, "C001", "test@test.com", "msg"));
    }

    @Test
    void should_create_customer() {
        Customer customer = new Customer(1L, "C001", "王小明", "wang@example.com", "0912-345-678");
        assertEquals("C001", customer.getCustomerId());
        assertEquals("王小明", customer.getName());
        assertEquals("wang@example.com", customer.getEmail());
        assertEquals("0912-345-678", customer.getPhone());
    }
}
