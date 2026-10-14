package com.smartshield.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Dedicated status audit history entity for SOS alerts.
 * Strictly separated from grievance status history.
 */
@Entity
@Table(name = "sos_status_history")
public class SosStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sos_alert_id", nullable = false)
    private SosAlert sosAlert;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private SosStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private SosStatus newStatus;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public SosStatusHistory() {
        this.changedAt = LocalDateTime.now();
    }

    public SosStatusHistory(SosAlert sosAlert, SosStatus oldStatus, SosStatus newStatus, String changedBy, String remarks) {
        this.sosAlert = sosAlert;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.remarks = remarks;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public SosAlert getSosAlert() {
        return sosAlert;
    }

    public void setSosAlert(SosAlert sosAlert) {
        this.sosAlert = sosAlert;
    }

    public SosStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(SosStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public SosStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(SosStatus newStatus) {
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
}
