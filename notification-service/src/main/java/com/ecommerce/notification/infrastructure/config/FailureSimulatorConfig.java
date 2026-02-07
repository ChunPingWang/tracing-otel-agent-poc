package com.ecommerce.notification.infrastructure.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FailureSimulatorConfig {

    private final AtomicBoolean failureEnabled = new AtomicBoolean(false);

    public void setFailureEnabled(boolean enabled) {
        failureEnabled.set(enabled);
    }

    public boolean isFailureEnabled() {
        return failureEnabled.get();
    }
}
