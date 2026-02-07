package com.ecommerce.order.application.service;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;
import com.ecommerce.order.application.dto.OrderItemCommand;
import com.ecommerce.order.application.port.in.CreateOrderPort;
import com.ecommerce.order.application.port.out.InventoryReleasePort;
import com.ecommerce.order.application.port.out.InventoryReservePort;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.application.port.out.ProductInfo;
import com.ecommerce.order.application.port.out.ProductQueryPort;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateOrderUseCase implements CreateOrderPort {

    private final ProductQueryPort productQueryPort;
    private final InventoryReservePort inventoryReservePort;
    private final InventoryReleasePort inventoryReleasePort;
    private final PaymentPort paymentPort;
    private final OrderRepository orderRepository;
    private final OrderEventPublisherPort orderEventPublisherPort;

    public CreateOrderUseCase(ProductQueryPort productQueryPort,
                              InventoryReservePort inventoryReservePort,
                              InventoryReleasePort inventoryReleasePort,
                              PaymentPort paymentPort,
                              OrderRepository orderRepository,
                              OrderEventPublisherPort orderEventPublisherPort) {
        this.productQueryPort = productQueryPort;
        this.inventoryReservePort = inventoryReservePort;
        this.inventoryReleasePort = inventoryReleasePort;
        this.paymentPort = paymentPort;
        this.orderRepository = orderRepository;
        this.orderEventPublisherPort = orderEventPublisherPort;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        List<OrderItem> orderItems = queryProductsAndBuildItems(command.getItems());
        String orderId = generateOrderId();
        Order order = Order.create(orderId, command.getCustomerId(), orderItems);
        order = orderRepository.save(order);

        try {
            reserveInventoryForItems(command.getItems());
        } catch (RuntimeException e) {
            return handleInventoryFailure(order);
        }

        try {
            processPaymentAndConfirm(order);
        } catch (ResourceAccessException e) {
            return handlePaymentTimeout(order, command.getItems());
        }

        order = orderRepository.save(order);
        return toResult(order);
    }

    private CreateOrderResult handleInventoryFailure(Order order) {
        order.fail();
        order = orderRepository.save(order);
        return toResult(order);
    }

    private CreateOrderResult handlePaymentTimeout(Order order,
                                                   List<OrderItemCommand> items) {
        releaseInventoryForItems(items);
        order.paymentTimeout();
        order = orderRepository.save(order);
        return toResult(order);
    }

    private List<OrderItem> queryProductsAndBuildItems(List<OrderItemCommand> itemCommands) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemCommand itemCmd : itemCommands) {
            ProductInfo product = productQueryPort.queryProduct(itemCmd.getProductId());
            orderItems.add(new OrderItem(
                    product.getProductId(),
                    itemCmd.getQuantity(),
                    product.getPrice()
            ));
        }
        return orderItems;
    }

    private void reserveInventoryForItems(List<OrderItemCommand> itemCommands) {
        for (OrderItemCommand itemCmd : itemCommands) {
            inventoryReservePort.reserveInventory(
                    itemCmd.getProductId(), itemCmd.getQuantity());
        }
    }

    private void releaseInventoryForItems(List<OrderItemCommand> itemCommands) {
        for (OrderItemCommand itemCmd : itemCommands) {
            inventoryReleasePort.releaseInventory(
                    itemCmd.getProductId(), itemCmd.getQuantity());
        }
    }

    private void processPaymentAndConfirm(Order order) {
        PaymentResult paymentResult = paymentPort.processPayment(
                order.getOrderId(), order.getTotalAmount());
        if (paymentResult.isSuccess()) {
            order.confirm();
        }
    }

    private CreateOrderResult toResult(Order order) {
        return new CreateOrderResult(
                order.getOrderId(),
                order.getStatus().name(),
                order.getTotalAmount()
        );
    }

    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString();
    }
}
