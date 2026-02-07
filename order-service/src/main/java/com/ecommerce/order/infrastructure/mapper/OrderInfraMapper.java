package com.ecommerce.order.infrastructure.mapper;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;
import com.ecommerce.order.application.dto.OrderItemCommand;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.infrastructure.adapter.in.rest.CreateOrderRequest;
import com.ecommerce.order.infrastructure.adapter.in.rest.CreateOrderResponse;
import com.ecommerce.order.infrastructure.dto.OrderConfirmedMessage;
import com.ecommerce.order.infrastructure.dto.OrderItemMessage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class OrderInfraMapper {

    private OrderInfraMapper() {}

    public static CreateOrderCommand toCommand(CreateOrderRequest request) {
        List<OrderItemCommand> items = request.getItems().stream()
                .map(item -> new OrderItemCommand(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());
        return new CreateOrderCommand(request.getCustomerId(), items);
    }

    public static CreateOrderResponse toResponse(CreateOrderResult result) {
        return new CreateOrderResponse(
                result.getOrderId(),
                result.getStatus(),
                result.getTotalAmount(),
                ""
        );
    }

    public static OrderConfirmedMessage toMessage(OrderConfirmedEvent event) {
        List<OrderItemMessage> items = event.getItems().stream()
                .map(item -> new OrderItemMessage(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .collect(Collectors.toList());

        OrderConfirmedMessage message = new OrderConfirmedMessage();
        message.setOrderId(event.getOrderId());
        message.setCustomerId(event.getCustomerId());
        message.setCustomerEmail("");
        message.setItems(items);
        message.setTotalAmount(event.getTotalAmount());
        message.setStatus("CONFIRMED");
        message.setTimestamp(event.getTimestamp()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return message;
    }
}
