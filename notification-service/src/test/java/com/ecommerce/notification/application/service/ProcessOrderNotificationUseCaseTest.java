package com.ecommerce.notification.application.service;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;
import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.domain.model.Customer;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.port.CustomerRepository;
import com.ecommerce.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessOrderNotificationUseCaseTest {

    private CustomerRepository customerRepository;
    private NotificationRepository notificationRepository;
    private NotificationSenderPort notificationSender;
    private ProcessOrderNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        notificationSender = mock(NotificationSenderPort.class);
        useCase = new ProcessOrderNotificationUseCase(
                customerRepository, notificationRepository, notificationSender);
    }

    @Test
    void should_process_notification_when_customer_found() {
        // Given
        Customer customer = new Customer(1L, "C001", "王小明", "wang@example.com", "0912-345-678");
        when(customerRepository.findByCustomerId("C001")).thenReturn(Optional.of(customer));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderNotificationCommand command = new OrderNotificationCommand(
                "ORD-001", "C001", new BigDecimal("1500.00"));

        // When
        useCase.processNotification(command);

        // Then
        verify(notificationSender).send(eq("wang@example.com"), contains("ORD-001"));
        verify(notificationRepository).save(argThat(notification ->
                notification.getStatus() == NotificationStatus.SENT
                        && "ORD-001".equals(notification.getOrderId())
                        && "wang@example.com".equals(notification.getCustomerEmail())));
    }

    @Test
    void should_save_failed_notification_when_sender_fails() {
        // Given
        Customer customer = new Customer(1L, "C001", "王小明", "wang@example.com", "0912-345-678");
        when(customerRepository.findByCustomerId("C001")).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("SMTP error")).when(notificationSender)
                .send(anyString(), anyString());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderNotificationCommand command = new OrderNotificationCommand(
                "ORD-001", "C001", new BigDecimal("1500.00"));

        // When
        useCase.processNotification(command);

        // Then
        verify(notificationRepository).save(argThat(notification ->
                notification.getStatus() == NotificationStatus.FAILED
                        && "ORD-001".equals(notification.getOrderId())));
    }

    @Test
    void should_use_customer_id_as_email_when_customer_not_found() {
        // Given
        when(customerRepository.findByCustomerId("C999")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderNotificationCommand command = new OrderNotificationCommand(
                "ORD-002", "C999", new BigDecimal("500.00"));

        // When
        useCase.processNotification(command);

        // Then
        verify(notificationSender).send(eq("C999"), contains("ORD-002"));
        verify(notificationRepository).save(argThat(notification ->
                notification.getStatus() == NotificationStatus.SENT
                        && "C999".equals(notification.getCustomerEmail())));
    }
}
