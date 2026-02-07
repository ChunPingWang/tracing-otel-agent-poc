package com.ecommerce.notification.infrastructure.adapter.in.rest;

import com.ecommerce.notification.infrastructure.config.FailureSimulatorConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final FailureSimulatorConfig failureConfig;

    public AdminController(FailureSimulatorConfig failureConfig) {
        this.failureConfig = failureConfig;
    }

    @PostMapping("/api/admin/simulate-failure")
    public ResponseEntity<String> simulateFailure(@RequestParam("enabled") boolean enabled) {
        failureConfig.setFailureEnabled(enabled);
        String status = enabled ? "enabled" : "disabled";
        return ResponseEntity.ok("Failure simulation: " + status);
    }
}
