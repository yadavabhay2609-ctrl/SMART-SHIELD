package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.SosAlert;

public class SosAlertResponse {

    private Long id;
    private Long citizenId;
    private String citizenName;
    private String citizenEmail;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private String source;
    private String message;
    private String status;
    private String notificationStatus;
    private String emailStatus;
    private String telegramStatus;
    private String notificationDetails;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer emergencyContactsCount;

    public SosAlertResponse() {
    }

    public SosAlertResponse(Long id, Long citizenId, String citizenName, String citizenEmail,
                            Double latitude, Double longitude, Double accuracy, String source,
                            String message, String status, String notificationStatus,
                            String notificationDetails, LocalDateTime triggeredAt,
                            LocalDateTime createdAt, LocalDateTime updatedAt,
                            Integer emergencyContactsCount) {
        this(id, citizenId, citizenName, citizenEmail, latitude, longitude, accuracy, source,
             message, status, notificationStatus, "NOT_CONFIGURED", "NOT_CONFIGURED",
             notificationDetails, triggeredAt, createdAt, updatedAt, emergencyContactsCount);
    }

    public SosAlertResponse(Long id, Long citizenId, String citizenName, String citizenEmail,
                            Double latitude, Double longitude, Double accuracy, String source,
                            String message, String status, String notificationStatus,
                            String emailStatus, String telegramStatus,
                            String notificationDetails, LocalDateTime triggeredAt,
                            LocalDateTime createdAt, LocalDateTime updatedAt,
                            Integer emergencyContactsCount) {
        this.id = id;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.citizenEmail = citizenEmail;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.source = source;
        this.message = message;
        this.status = status;
        this.notificationStatus = notificationStatus;
        this.emailStatus = emailStatus;
        this.telegramStatus = telegramStatus;
        this.notificationDetails = notificationDetails;
        this.triggeredAt = triggeredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.emergencyContactsCount = emergencyContactsCount;
    }

    public static SosAlertResponse fromEntity(SosAlert alert, Integer contactsCount) {
        if (alert == null) {
            return null;
        }
        return new SosAlertResponse(
                alert.getId(),
                alert.getCitizen() != null ? alert.getCitizen().getId() : null,
                alert.getCitizen() != null ? alert.getCitizen().getFullName() : null,
                alert.getCitizen() != null ? alert.getCitizen().getEmail() : null,
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getAccuracy(),
                alert.getSource(),
                alert.getMessage(),
                alert.getStatus() != null ? alert.getStatus().name() : null,
                alert.getNotificationStatus() != null ? alert.getNotificationStatus().name() : null,
                alert.getEmailStatus() != null ? alert.getEmailStatus() : "NOT_CONFIGURED",
                alert.getTelegramStatus() != null ? alert.getTelegramStatus() : "NOT_CONFIGURED",
                alert.getNotificationDetails(),
                alert.getTriggeredAt(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                contactsCount
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(Long citizenId) {
        this.citizenId = citizenId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getCitizenEmail() {
        return citizenEmail;
    }

    public void setCitizenEmail(String citizenEmail) {
        this.citizenEmail = citizenEmail;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getEmergencyContactsCount() {
        return emergencyContactsCount;
    }

    public void setEmergencyContactsCount(Integer emergencyContactsCount) {
        this.emergencyContactsCount = emergencyContactsCount;
    }
}
