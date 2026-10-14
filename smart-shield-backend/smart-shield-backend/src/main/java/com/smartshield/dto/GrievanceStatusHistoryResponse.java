package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.GrievanceStatus;
import com.smartshield.model.GrievanceStatusHistory;

public class GrievanceStatusHistoryResponse {

    private Long id;
    private GrievanceStatus oldStatus;
    private GrievanceStatus newStatus;
    private String changedBy;
    private String remarks;
    private LocalDateTime changedAt;

    public GrievanceStatusHistoryResponse() {
    }

    public GrievanceStatusHistoryResponse(Long id, GrievanceStatus oldStatus, GrievanceStatus newStatus,
                                          String changedBy, String remarks, LocalDateTime changedAt) {
        this.id = id;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.remarks = remarks;
        this.changedAt = changedAt;
    }

    public static GrievanceStatusHistoryResponse fromEntity(GrievanceStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new GrievanceStatusHistoryResponse(
                history.getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedBy(),
                history.getRemarks(),
                history.getChangedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GrievanceStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(GrievanceStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public GrievanceStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(GrievanceStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
