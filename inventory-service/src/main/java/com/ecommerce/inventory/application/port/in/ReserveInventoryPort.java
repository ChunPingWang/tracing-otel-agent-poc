package com.ecommerce.inventory.application.port.in;

import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;

public interface ReserveInventoryPort {
    ReserveResult reserve(ReserveCommand command);
}
