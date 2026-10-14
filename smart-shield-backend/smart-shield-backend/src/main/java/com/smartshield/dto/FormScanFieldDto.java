package com.smartshield.dto;

import java.util.Objects;

/**
 * Represents an individual extracted field from a scanned government form.
 */
public class FormScanFieldDto {

    private String label;
    private String name;
    private String value;
    private String status; // FILLED, EMPTY, UNCLEAR, NOT_APPLICABLE
    private Integer confidence; // 0 - 100
    private String guidance;
    private boolean verificationRequired;

    public FormScanFieldDto() {
    }

    public FormScanFieldDto(String label, String name, String value, String status, Integer confidence, String guidance, boolean verificationRequired) {
        this.label = label;
        this.name = name;
        this.value = value;
        this.status = status;
        this.confidence = confidence;
        this.guidance = guidance;
        this.verificationRequired = verificationRequired;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getGuidance() {
        return guidance;
    }

    public void setGuidance(String guidance) {
        this.guidance = guidance;
    }

    public boolean isVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormScanFieldDto that)) return false;
        return verificationRequired == that.verificationRequired &&
                Objects.equals(label, that.label) &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(status, that.status) &&
                Objects.equals(confidence, that.confidence) &&
                Objects.equals(guidance, that.guidance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, value, status, confidence, guidance, verificationRequired);
    }
}
