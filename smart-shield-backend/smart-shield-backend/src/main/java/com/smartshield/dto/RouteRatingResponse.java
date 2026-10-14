package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.SafeRouteRating;

public class RouteRatingResponse {

    private Long id;
    private String safeRouteId;
    private String citizenName;
    private Integer rating;
    private String comment;
    private String safetyTag;
    private LocalDateTime createdAt;

    public RouteRatingResponse() {
    }

    public static RouteRatingResponse fromEntity(SafeRouteRating entity) {
        if (entity == null) {
            return null;
        }
        RouteRatingResponse response = new RouteRatingResponse();
        response.setId(entity.getId());
        if (entity.getRoute() != null) {
            response.setSafeRouteId(entity.getRoute().getSafeRouteId());
        }
        if (entity.getCitizen() != null) {
            String name = entity.getCitizen().getFullName();
            if (name != null && !name.isBlank()) {
                // Privacy masking: e.g. "Rahul S." or "Citizen"
                String[] parts = name.trim().split("\\s+");
                if (parts.length > 1) {
                    response.setCitizenName(parts[0] + " " + parts[1].charAt(0) + ".");
                } else {
                    response.setCitizenName(parts[0]);
                }
            } else {
                response.setCitizenName("Verified Citizen");
            }
        } else {
            response.setCitizenName("Verified Citizen");
        }
        response.setRating(entity.getRating());
        response.setComment(entity.getComment());
        response.setSafetyTag(entity.getSafetyTag());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSafeRouteId() {
        return safeRouteId;
    }

    public void setSafeRouteId(String safeRouteId) {
        this.safeRouteId = safeRouteId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSafetyTag() {
        return safetyTag;
    }

    public void setSafetyTag(String safetyTag) {
        this.safetyTag = safetyTag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
