package com.ecommerce.inventory.infrastructure.adapter.in.rest;

import com.ecommerce.inventory.application.dto.ReleaseCommand;
import com.ecommerce.inventory.application.dto.ReserveCommand;
import com.ecommerce.inventory.application.dto.ReserveResult;
import com.ecommerce.inventory.application.port.in.ReleaseInventoryPort;
import com.ecommerce.inventory.application.port.in.ReserveInventoryPort;
import com.ecommerce.inventory.domain.model.InsufficientStockException;
import com.ecommerce.inventory.infrastructure.dto.ReleaseRequest;
import com.ecommerce.inventory.infrastructure.dto.ReleaseResponse;
import com.ecommerce.inventory.infrastructure.dto.ReserveRequest;
import com.ecommerce.inventory.infrastructure.dto.ReserveResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ReserveInventoryPort reserveInventoryPort;
    private final ReleaseInventoryPort releaseInventoryPort;

    public InventoryController(ReserveInventoryPort reserveInventoryPort,
                               ReleaseInventoryPort releaseInventoryPort) {
        this.reserveInventoryPort = reserveInventoryPort;
        this.releaseInventoryPort = releaseInventoryPort;
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        try {
            ReserveResult result = reserveInventoryPort.reserve(
                    new ReserveCommand(request.getProductId(), request.getQuantity()));
            return ResponseEntity.ok(new ReserveResponse(result.isReserved(), result.getRemainingStock()));
        } catch (InsufficientStockException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PostMapping("/release")
    public ResponseEntity<ReleaseResponse> release(@RequestBody ReleaseRequest request) {
        releaseInventoryPort.release(
                new ReleaseCommand(request.getProductId(), request.getQuantity()));
        return ResponseEntity.ok(new ReleaseResponse(true));
    }
}
