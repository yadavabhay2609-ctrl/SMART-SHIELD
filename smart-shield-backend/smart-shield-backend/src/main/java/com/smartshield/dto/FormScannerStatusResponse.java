package com.smartshield.dto;

import java.util.List;

/**
 * Public configuration and health status DTO for Government Form Scanner.
 */
public class FormScannerStatusResponse {

    private boolean enabled;
    private boolean configured;
    private String model;
    private List<String> supportedMimeTypes;
    private long maxFileSizeBytes;
    private boolean documentStorageEnabled = false;
    private String message;

    public FormScannerStatusResponse() {
    }

    public FormScannerStatusResponse(boolean enabled, boolean configured, String model, List<String> supportedMimeTypes, long maxFileSizeBytes, String message) {
        this.enabled = enabled;
        this.configured = configured;
        this.model = model;
        this.supportedMimeTypes = supportedMimeTypes;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.documentStorageEnabled = false;
        this.message = message;
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

    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public boolean isDocumentStorageEnabled() {
        return documentStorageEnabled;
    }

    public void setDocumentStorageEnabled(boolean documentStorageEnabled) {
        this.documentStorageEnabled = documentStorageEnabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
