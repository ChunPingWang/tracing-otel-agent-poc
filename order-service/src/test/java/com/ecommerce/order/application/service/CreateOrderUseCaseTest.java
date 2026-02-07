package com.ecommerce.order.application.service;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;
import com.ecommerce.order.application.dto.OrderItemCommand;
import com.ecommerce.order.application.port.out.InventoryReleasePort;
import com.ecommerce.order.application.port.out.InventoryReservePort;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.application.port.out.ProductInfo;
import com.ecommerce.order.application.port.out.ProductQueryPort;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CreateOrderUseCaseTest {

    private ProductQueryPort productQueryPort;
    private InventoryReservePort inventoryReservePort;
    private InventoryReleasePort inventoryReleasePort;
    private PaymentPort paymentPort;
    private OrderRepository orderRepository;
    private OrderEventPublisherPort orderEventPublisherPort;
    private CreateOrderUseCase createOrderUseCase;

    @BeforeEach
    void setUp() {
        productQueryPort = mock(ProductQueryPort.class);
        inventoryReservePort = mock(InventoryReservePort.class);
        inventoryReleasePort = mock(InventoryReleasePort.class);
        paymentPort = mock(PaymentPort.class);
        orderRepository = mock(OrderRepository.class);
        orderEventPublisherPort = mock(OrderEventPublisherPort.class);
        createOrderUseCase = new CreateOrderUseCase(
                productQueryPort,
                inventoryReservePort,
                inventoryReleasePort,
                paymentPort,
                orderRepository,
                orderEventPublisherPort
        );
    }

    @Test
    void should_create_order_with_confirmed_status_when_all_steps_succeed() {
        // Given
        ProductInfo product = new ProductInfo("P001", "Laptop", new BigDecimal("999.00"));
        when(productQueryPort.queryProduct("P001")).thenReturn(product);
        when(inventoryReservePort.reserveInventory("P001", 2)).thenReturn(true);
        when(paymentPort.processPayment(anyString(), eq(new BigDecimal("1998.00"))))
                .thenReturn(new PaymentResult("PAY-001", "SUCCESS"));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand("C001",
                Arrays.asList(new OrderItemCommand("P001", 2)));

        // When
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        // Then
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("ORD-"));
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(new BigDecimal("1998.00"), result.getTotalAmount());

        // Verify interactions
        verify(productQueryPort).queryProduct("P001");
        verify(inventoryReservePort).reserveInventory("P001", 2);
        verify(paymentPort).processPayment(anyString(), eq(new BigDecimal("1998.00")));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order finalOrder = orderCaptor.getAllValues().get(1);
        assertEquals(OrderStatus.CONFIRMED, finalOrder.getStatus());

        // Verify event published
        ArgumentCaptor<OrderConfirmedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(orderEventPublisherPort).publish(eventCaptor.capture());
        OrderConfirmedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("C001", publishedEvent.getCustomerId());
        assertEquals(new BigDecimal("1998.00"), publishedEvent.getTotalAmount());
        assertEquals(1, publishedEvent.getItems().size());
    }

    @Test
    void should_query_product_for_each_item() {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(productQueryPort.queryProduct("P002"))
                .thenReturn(new ProductInfo("P002", "Mouse", new BigDecimal("29.00")));
        when(inventoryReservePort.reserveInventory(anyString(), anyInt())).thenReturn(true);
        when(paymentPort.processPayment(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult("PAY-002", "SUCCESS"));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand("C001",
                Arrays.asList(
                        new OrderItemCommand("P001", 1),
                        new OrderItemCommand("P002", 3)
                ));

        // When
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        // Then
        assertEquals(new BigDecimal("1086.00"), result.getTotalAmount());
        verify(productQueryPort).queryProduct("P001");
        verify(productQueryPort).queryProduct("P002");
        verify(inventoryReservePort).reserveInventory("P001", 1);
        verify(inventoryReservePort).reserveInventory("P002", 3);
    }

    @Test
    void should_save_order_as_created_before_processing() {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(inventoryReservePort.reserveInventory("P001", 1)).thenReturn(true);
        when(paymentPort.processPayment(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult("PAY-003", "SUCCESS"));

        // Capture status at the time of each save call
        java.util.List<OrderStatus> savedStatuses = new java.util.ArrayList<>();
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    savedStatuses.add(order.getStatus());
                    return order;
                });

        CreateOrderCommand command = new CreateOrderCommand("C001",
                Arrays.asList(new OrderItemCommand("P001", 1)));

        // When
        createOrderUseCase.createOrder(command);

        // Then: first save should be CREATED, second should be CONFIRMED
        assertEquals(2, savedStatuses.size());
        assertEquals(OrderStatus.CREATED, savedStatuses.get(0));
        assertEquals(OrderStatus.CONFIRMED, savedStatuses.get(1));
    }

    @Test
    void should_fail_order_when_inventory_insufficient() {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(inventoryReservePort.reserveInventory("P001", 2))
                .thenThrow(new RuntimeException("409 Conflict: Inventory insufficient"));

        java.util.List<OrderStatus> savedStatuses = new java.util.ArrayList<>();
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    savedStatuses.add(o.getStatus());
                    return o;
                });

        CreateOrderCommand command = new CreateOrderCommand("C001",
                Arrays.asList(new OrderItemCommand("P001", 2)));

        // When
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        // Then
        assertEquals("FAILED", result.getStatus());
        verify(paymentPort, never()).processPayment(anyString(), any(BigDecimal.class));
        verify(orderEventPublisherPort, never()).publish(any(OrderConfirmedEvent.class));
        assertEquals(2, savedStatuses.size());
        assertEquals(OrderStatus.CREATED, savedStatuses.get(0));
        assertEquals(OrderStatus.FAILED, savedStatuses.get(1));
    }

    @Test
    void should_timeout_order_and_release_inventory_when_payment_times_out() {
        // Given
        when(productQueryPort.queryProduct("P001"))
                .thenReturn(new ProductInfo("P001", "Laptop", new BigDecimal("999.00")));
        when(productQueryPort.queryProduct("P002"))
                .thenReturn(new ProductInfo("P002", "Mouse", new BigDecimal("29.00")));
        when(inventoryReservePort.reserveInventory(anyString(), anyInt())).thenReturn(true);
        when(paymentPort.processPayment(anyString(), any(BigDecimal.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        java.util.List<OrderStatus> savedStatuses = new java.util.ArrayList<>();
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    savedStatuses.add(o.getStatus());
                    return o;
                });

        CreateOrderCommand command = new CreateOrderCommand("C001",
                Arrays.asList(
                        new OrderItemCommand("P001", 1),
                        new OrderItemCommand("P002", 3)
                ));

        // When
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        // Then
        assertEquals("PAYMENT_TIMEOUT", result.getStatus());
        verify(inventoryReleasePort).releaseInventory("P001", 1);
        verify(inventoryReleasePort).releaseInventory("P002", 3);
        verify(orderEventPublisherPort, never()).publish(any(OrderConfirmedEvent.class));
        assertEquals(2, savedStatuses.size());
        assertEquals(OrderStatus.CREATED, savedStatuses.get(0));
        assertEquals(OrderStatus.PAYMENT_TIMEOUT, savedStatuses.get(1));
    }
}
