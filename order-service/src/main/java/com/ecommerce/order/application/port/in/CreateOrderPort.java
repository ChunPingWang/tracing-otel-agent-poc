package com.ecommerce.order.application.port.in;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;

public interface CreateOrderPort {
    CreateOrderResult createOrder(CreateOrderCommand command);
}
