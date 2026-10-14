package com.smartshield.dto;

import com.smartshield.model.GrievanceStatus;

import jakarta.validation.constraints.NotNull;

public class UpdateGrievanceStatusRequest {

    @NotNull(message = "Status is required")
    private GrievanceStatus status;

    private String remarks;

    public UpdateGrievanceStatusRequest() {
    }

    public UpdateGrievanceStatusRequest(GrievanceStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public GrievanceStatus getStatus() {
        return status;
    }

    public void setStatus(GrievanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
