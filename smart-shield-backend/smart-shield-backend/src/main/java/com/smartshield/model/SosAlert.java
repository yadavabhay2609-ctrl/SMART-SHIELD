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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entity representing an emergency SOS alert triggered by a citizen.
 */
@Entity
@Table(name = "sos_alerts")
public class SosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "source", nullable = false, length = 50)
    private String source = "BROWSER";

    @Column(name = "message", length = 500)
    private String message = "Emergency assistance required";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SosStatus status = SosStatus.TRIGGERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 30)
    private SosNotificationStatus notificationStatus = SosNotificationStatus.PENDING;

    @Column(name = "email_status", length = 30)
    private String emailStatus = "NOT_CONFIGURED";

    @Column(name = "telegram_status", length = 30)
    private String telegramStatus = "NOT_CONFIGURED";

    @Column(name = "notification_details", length = 1000)
    private String notificationDetails;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SosAlert() {
        this.triggeredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = SosStatus.TRIGGERED;
        this.notificationStatus = SosNotificationStatus.PENDING;
    }

    public SosAlert(User citizen, Double latitude, Double longitude, Double accuracy, String source, String message) {
        this.citizen = citizen;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.source = (source != null && !source.isBlank()) ? source : "BROWSER";
        this.message = (message != null && !message.isBlank()) ? message : "Emergency assistance required";
        this.status = SosStatus.TRIGGERED;
        this.notificationStatus = SosNotificationStatus.PENDING;
        this.triggeredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.triggeredAt == null) {
            this.triggeredAt = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getCitizen() {
        return citizen;
    }

    public void setCitizen(User citizen) {
        this.citizen = citizen;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SosStatus getStatus() {
        return status;
    }

    public void setStatus(SosStatus status) {
        this.status = status;
    }

    public SosNotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(SosNotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public String getNotificationDetails() {
        return notificationDetails;
    }

    public void setNotificationDetails(String notificationDetails) {
        this.notificationDetails = notificationDetails;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(String emailStatus) {
        this.emailStatus = emailStatus;
    }

    public String getTelegramStatus() {
        return telegramStatus;
    }

    public void setTelegramStatus(String telegramStatus) {
        this.telegramStatus = telegramStatus;
    }
}
