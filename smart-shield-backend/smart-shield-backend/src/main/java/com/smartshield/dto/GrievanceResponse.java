package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.Grievance;
import com.smartshield.model.GrievanceCategory;
import com.smartshield.model.GrievancePriority;
import com.smartshield.model.GrievanceStatus;

public class GrievanceResponse {

    private Long id;
    private String trackingId;
    private Long citizenId;
    private String citizenName;
    private String citizenEmail;
    private GrievanceCategory category;
    private GrievancePriority priority;
    private GrievanceStatus status;
    private String title;
    private String description;
    private String location;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GrievanceResponse() {
    }

    public GrievanceResponse(Long id, String trackingId, Long citizenId, GrievanceCategory category,
                             GrievancePriority priority, GrievanceStatus status, String title,
                             String description, String location, String source,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.trackingId = trackingId;
        this.citizenId = citizenId;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.title = title;
        this.description = description;
        this.location = location;
        this.source = source;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static GrievanceResponse fromEntity(Grievance grievance) {
        if (grievance == null) {
            return null;
        }
        Long citizenId = (grievance.getCitizen() != null) ? grievance.getCitizen().getId() : null;
        String citizenName = (grievance.getCitizen() != null) ? grievance.getCitizen().getFullName() : null;
        String citizenEmail = (grievance.getCitizen() != null) ? grievance.getCitizen().getEmail() : null;
        GrievanceResponse response = new GrievanceResponse(
                grievance.getId(),
                grievance.getTrackingId(),
                citizenId,
                grievance.getCategory(),
                grievance.getPriority(),
                grievance.getStatus(),
                grievance.getTitle(),
                grievance.getDescription(),
                grievance.getLocation(),
                grievance.getSource(),
                grievance.getCreatedAt(),
                grievance.getUpdatedAt()
        );
        response.setCitizenName(citizenName);
        response.setCitizenEmail(citizenEmail);
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
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

    public GrievanceCategory getCategory() {
        return category;
    }

    public void setCategory(GrievanceCategory category) {
        this.category = category;
    }

    public GrievancePriority getPriority() {
        return priority;
    }

    public void setPriority(GrievancePriority priority) {
        this.priority = priority;
    }

    public GrievanceStatus getStatus() {
        return status;
    }

    public void setStatus(GrievanceStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
}
