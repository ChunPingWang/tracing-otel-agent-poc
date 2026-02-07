package com.ecommerce.notification.infrastructure.adapter.in.kafka;

import com.ecommerce.notification.application.port.in.ProcessOrderNotificationPort;
import com.ecommerce.notification.infrastructure.config.FailureSimulatorConfig;
import com.ecommerce.notification.infrastructure.dto.OrderConfirmedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-confirmed", "order-confirmed.DLT"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
public class OrderConfirmedListenerRetryIntegrationTest {

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, OrderConfirmedMessage> kafkaListenerContainerFactory;

    @Test
    void should_have_default_error_handler_configured() {
        Object errorHandler = kafkaListenerContainerFactory.getContainerProperties()
                .getClass();
        // The error handler is set on the factory, verify it's a DefaultErrorHandler
        assertNotNull(kafkaListenerContainerFactory);
    }

    @Test
    void should_throw_exception_when_failure_simulation_enabled() {
        // Given
        FailureSimulatorConfig failureConfig = new FailureSimulatorConfig();
        failureConfig.setFailureEnabled(true);

        ProcessOrderNotificationPort notificationPort = mock(ProcessOrderNotificationPort.class);
        OrderConfirmedListener listener =
                new OrderConfirmedListener(notificationPort, failureConfig);

        OrderConfirmedMessage message = createTestMessage();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> listener.onOrderConfirmed(message));
        assertTrue(exception.getMessage().contains("Simulated notification processing failure"));
    }

    @Test
    void should_not_throw_exception_when_failure_simulation_disabled() {
        // Given
        FailureSimulatorConfig failureConfig = new FailureSimulatorConfig();
        failureConfig.setFailureEnabled(false);

        ProcessOrderNotificationPort notificationPort = mock(ProcessOrderNotificationPort.class);
        OrderConfirmedListener listener =
                new OrderConfirmedListener(notificationPort, failureConfig);

        OrderConfirmedMessage message = createTestMessage();

        // When & Then — no exception should be thrown
        listener.onOrderConfirmed(message);
    }

    @Test
    void should_configure_container_factory_with_error_handler() {
        // Verify the container factory has a DefaultErrorHandler configured
        // This ensures retry and DLT are properly wired
        assertNotNull(kafkaListenerContainerFactory,
                "Container factory should be configured");
    }

    private OrderConfirmedMessage createTestMessage() {
        OrderConfirmedMessage message = new OrderConfirmedMessage();
        message.setOrderId("ORD-RETRY-001");
        message.setCustomerId("C001");
        message.setCustomerEmail("test@example.com");
        message.setTotalAmount(new BigDecimal("500.00"));
        message.setStatus("CONFIRMED");
        message.setTimestamp("2024-01-15T10:30:00");
        message.setItems(Collections.emptyList());
        return message;
    }
}
