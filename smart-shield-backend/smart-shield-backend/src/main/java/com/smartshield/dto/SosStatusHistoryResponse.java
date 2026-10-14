package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.SosStatusHistory;

public class SosStatusHistoryResponse {

    private Long id;
    private Long sosAlertId;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private String remarks;
    private LocalDateTime changedAt;

    public SosStatusHistoryResponse() {
    }

    public SosStatusHistoryResponse(Long id, Long sosAlertId, String oldStatus, String newStatus,
                                   String changedBy, String remarks, LocalDateTime changedAt) {
        this.id = id;
        this.sosAlertId = sosAlertId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.remarks = remarks;
        this.changedAt = changedAt;
    }

    public static SosStatusHistoryResponse fromEntity(SosStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new SosStatusHistoryResponse(
                history.getId(),
                history.getSosAlert() != null ? history.getSosAlert().getId() : null,
                history.getOldStatus() != null ? history.getOldStatus().name() : null,
                history.getNewStatus() != null ? history.getNewStatus().name() : null,
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

    public Long getSosAlertId() {
        return sosAlertId;
    }

    public void setSosAlertId(Long sosAlertId) {
        this.sosAlertId = sosAlertId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
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
