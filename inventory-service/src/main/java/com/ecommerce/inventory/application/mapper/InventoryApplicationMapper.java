package com.ecommerce.inventory.application.mapper;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;

public class InventoryApplicationMapper {
    public static ReserveResult toResult(boolean reserved, int remainingStock) {
        return new ReserveResult(reserved, remainingStock);
    }
}
