package com.ecommerce.payment.infrastructure.adapter.in.rest;

import com.ecommerce.payment.infrastructure.config.DelaySimulatorConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for Payment Service. Provides delay simulation for timeout testing.
 */
@RestController
public class AdminController {

    private final DelaySimulatorConfig delayConfig;

    public AdminController(DelaySimulatorConfig delayConfig) {
        this.delayConfig = delayConfig;
    }

    /** Sets the simulated processing delay in milliseconds. Use 0 to disable. */
    @PostMapping("/api/admin/simulate-delay")
    public ResponseEntity<String> simulateDelay(@RequestParam("ms") int ms) {
        delayConfig.setDelayMs(ms);
        return ResponseEntity.ok("Delay set to " + ms + "ms");
    }
}
