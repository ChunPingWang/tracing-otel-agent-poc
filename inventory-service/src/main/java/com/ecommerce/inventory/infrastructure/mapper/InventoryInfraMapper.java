package com.ecommerce.inventory.infrastructure.mapper;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.infrastructure.dto.ReserveRequest;

public class InventoryInfraMapper {
    public static ReserveCommand toCommand(ReserveRequest request) {
        return new ReserveCommand(request.getProductId(), request.getQuantity());
    }
}
