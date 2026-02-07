package com.ecommerce.notification.application.service;

import com.ecommerce.notification.application.dto.OrderNotificationCommand;
import com.ecommerce.notification.application.port.in.ProcessOrderNotificationPort;
import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.domain.model.Customer;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.port.CustomerRepository;
import com.ecommerce.notification.domain.port.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ProcessOrderNotificationUseCase implements ProcessOrderNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(ProcessOrderNotificationUseCase.class);

    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSenderPort notificationSender;

    public ProcessOrderNotificationUseCase(CustomerRepository customerRepository,
                                           NotificationRepository notificationRepository,
                                           NotificationSenderPort notificationSender) {
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    @Override
    public void processNotification(OrderNotificationCommand command) {
        String email = resolveCustomerEmail(command.getCustomerId());
        String message = buildMessage(command);
        trySendAndSave(command, email, message);
    }

    private String resolveCustomerEmail(String customerId) {
        Optional<Customer> customer = customerRepository.findByCustomerId(customerId);
        if (!customer.isPresent()) {
            log.warn("Customer not found: {}, using customerId as email placeholder", customerId);
            return customerId;
        }
        return customer.get().getEmail();
    }

    private String buildMessage(OrderNotificationCommand command) {
        return String.format("訂單 %s 已確認，金額 %s",
                command.getOrderId(), command.getTotalAmount());
    }

    private void trySendAndSave(OrderNotificationCommand command,
                                String email, String message) {
        try {
            notificationSender.send(email, message);
            Notification notification = Notification.createSent(
                    command.getOrderId(), command.getCustomerId(), email, message);
            notificationRepository.save(notification);
            log.info("Notification sent for order: {}", command.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send notification for order: {}", command.getOrderId(), e);
            Notification notification = Notification.createFailed(
                    command.getOrderId(), command.getCustomerId(), email, message);
            notificationRepository.save(notification);
        }
    }
}
