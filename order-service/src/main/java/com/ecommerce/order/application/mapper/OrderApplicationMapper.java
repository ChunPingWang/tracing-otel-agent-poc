package com.ecommerce.order.application.mapper;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderItemCommand;
import com.ecommerce.order.application.port.out.ProductInfo;
import com.ecommerce.order.domain.model.OrderItem;

public final class OrderApplicationMapper {

    private OrderApplicationMapper() {}

    public static OrderItem toOrderItem(OrderItemCommand command, ProductInfo productInfo) {
        return new OrderItem(
                productInfo.getProductId(),
                command.getQuantity(),
                productInfo.getPrice()
        );
    }
}
