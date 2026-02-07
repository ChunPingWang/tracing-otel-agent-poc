package com.ecommerce.inventory.application.port.in;

import com.ecommerce.inventory.application.dto.ReleaseCommand;

public interface ReleaseInventoryPort {
    void release(ReleaseCommand command);
}
