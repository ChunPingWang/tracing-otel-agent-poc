package com.ecommerce.order.infrastructure.mapper;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;
import com.ecommerce.order.application.dto.OrderItemCommand;
import com.ecommerce.order.infrastructure.adapter.in.rest.CreateOrderRequest;
import com.ecommerce.order.infrastructure.adapter.in.rest.CreateOrderResponse;

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
}
