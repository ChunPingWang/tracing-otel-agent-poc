package com.ecommerce.payment.infrastructure.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DelaySimulatorConfig {

    private final AtomicInteger delayMs = new AtomicInteger(0);

    public void setDelayMs(int ms) {
        delayMs.set(ms);
    }

    public int getDelayMs() {
        return delayMs.get();
    }

    public void applyDelay() {
        int delay = delayMs.get();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
