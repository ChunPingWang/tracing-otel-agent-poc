package com.ecommerce.inventory.infrastructure.dto;

public class ReleaseResponse {
    private boolean released;

    public ReleaseResponse() {}
    public ReleaseResponse(boolean released) { this.released = released; }
    public boolean isReleased() { return released; }
    public void setReleased(boolean released) { this.released = released; }
}
