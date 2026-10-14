package com.smartshield.dto;

import com.smartshield.model.SosStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateSosStatusRequest {

    @NotNull(message = "New status is required")
    private SosStatus status;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    public UpdateSosStatusRequest() {
    }

    public UpdateSosStatusRequest(SosStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public SosStatus getStatus() {
        return status;
    }

    public void setStatus(SosStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
