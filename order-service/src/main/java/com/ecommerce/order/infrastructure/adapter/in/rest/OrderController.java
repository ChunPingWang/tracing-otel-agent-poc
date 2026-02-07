package com.ecommerce.order.infrastructure.adapter.in.rest;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.CreateOrderResult;
import com.ecommerce.order.application.port.in.CreateOrderPort;
import com.ecommerce.order.infrastructure.mapper.OrderInfraMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderPort createOrderPort;

    public OrderController(CreateOrderPort createOrderPort) {
        this.createOrderPort = createOrderPort;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = OrderInfraMapper.toCommand(request);
        CreateOrderResult result = createOrderPort.createOrder(command);
        CreateOrderResponse response = OrderInfraMapper.toResponse(result);
        return ResponseEntity.ok(response);
    }
}
