package com.smartshield.dto;

public class AiStatusResponse {

    private boolean enabled;
    private boolean configured;
    private String model;
    private boolean localKbAvailable;
    private String statusMessage;

    public AiStatusResponse() {
    }

    public AiStatusResponse(boolean enabled, boolean configured, String model,
                            boolean localKbAvailable, String statusMessage) {
        this.enabled = enabled;
        this.configured = configured;
        this.model = model;
        this.localKbAvailable = localKbAvailable;
        this.statusMessage = statusMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isLocalKbAvailable() {
        return localKbAvailable;
    }

    public void setLocalKbAvailable(boolean localKbAvailable) {
        this.localKbAvailable = localKbAvailable;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
