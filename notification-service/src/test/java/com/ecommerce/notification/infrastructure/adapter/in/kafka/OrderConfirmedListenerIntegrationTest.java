package com.ecommerce.notification.infrastructure.adapter.in.kafka;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;
import com.ecommerce.notification.application.port.in.ProcessOrderNotificationPort;
import com.ecommerce.notification.infrastructure.dto.OrderConfirmedMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class OrderConfirmedListenerIntegrationTest {

    @Test
    void should_consume_order_confirmed_event_and_delegate_to_port() {
        // Given
        ProcessOrderNotificationPort notificationPort = mock(ProcessOrderNotificationPort.class);
        OrderConfirmedListener listener = new OrderConfirmedListener(notificationPort);

        OrderConfirmedMessage message = new OrderConfirmedMessage();
        message.setOrderId("ORD-001");
        message.setCustomerId("C001");
        message.setCustomerEmail("wang@example.com");
        message.setTotalAmount(new BigDecimal("1500.00"));
        message.setStatus("CONFIRMED");
        message.setTimestamp("2024-01-15T10:30:00");
        message.setItems(Collections.emptyList());

        // When
        listener.onOrderConfirmed(message);

        // Then
        verify(notificationPort).processNotification(argThat(command ->
                "ORD-001".equals(command.getOrderId())
                        && "C001".equals(command.getCustomerId())
                        && new BigDecimal("1500.00").equals(command.getTotalAmount())));
    }

    @Test
    void should_map_message_fields_correctly() {
        // Given
        ProcessOrderNotificationPort notificationPort = mock(ProcessOrderNotificationPort.class);
        OrderConfirmedListener listener = new OrderConfirmedListener(notificationPort);

        OrderConfirmedMessage message = new OrderConfirmedMessage();
        message.setOrderId("ORD-999");
        message.setCustomerId("C002");
        message.setCustomerEmail("lee@example.com");
        message.setTotalAmount(new BigDecimal("3200.50"));
        message.setStatus("CONFIRMED");
        message.setTimestamp("2024-01-15T11:00:00");
        message.setItems(Collections.emptyList());

        // When
        listener.onOrderConfirmed(message);

        // Then
        verify(notificationPort).processNotification(argThat(command ->
                "ORD-999".equals(command.getOrderId())
                        && "C002".equals(command.getCustomerId())
                        && new BigDecimal("3200.50").equals(command.getTotalAmount())));
    }
}
