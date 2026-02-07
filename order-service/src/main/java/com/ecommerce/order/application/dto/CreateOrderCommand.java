package com.ecommerce.order.application.dto;

import java.util.List;

public class CreateOrderCommand {
    private final String customerId;
    private final List<OrderItemCommand> items;

    public CreateOrderCommand(String customerId, List<OrderItemCommand> items) {
        this.customerId = customerId;
        this.items = items;
    }

    public String getCustomerId() { return customerId; }
    public List<OrderItemCommand> getItems() { return items; }
}
