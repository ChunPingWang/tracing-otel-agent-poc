package com.ecommerce.notification.infrastructure.adapter.out.persistence;

import com.ecommerce.notification.domain.model.Customer;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.port.CustomerRepository;
import com.ecommerce.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaNotificationRepositoryAdapter.class, JpaCustomerRepositoryAdapter.class})
@Sql(statements = {
    "INSERT INTO customers (customer_id, name, email, phone) VALUES ('C001', '王小明', 'wang@example.com', '0912-345-678')",
    "INSERT INTO customers (customer_id, name, email, phone) VALUES ('C002', '李小華', 'lee@example.com', '0923-456-789')"
})
public class JpaNotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void should_save_notification() {
        Notification notification = Notification.createSent("ORD-001", "C001",
                "wang@example.com", "訂單 ORD-001 已確認");
        Notification saved = notificationRepository.save(notification);
        assertNotNull(saved.getId());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
    }

    @Test
    void should_find_customer_by_customer_id() {
        Optional<Customer> customer = customerRepository.findByCustomerId("C001");
        assertTrue(customer.isPresent());
        assertEquals("王小明", customer.get().getName());
        assertEquals("wang@example.com", customer.get().getEmail());
    }

    @Test
    void should_return_empty_for_nonexistent_customer() {
        Optional<Customer> customer = customerRepository.findByCustomerId("C999");
        assertFalse(customer.isPresent());
    }
}
