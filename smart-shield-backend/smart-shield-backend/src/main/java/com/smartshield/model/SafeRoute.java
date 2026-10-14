package com.smartshield.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entity representing an analyzed safe route in Smart Shield.
 * Stores route geometry, navigation metrics, Smart Shield safety score, and audit flags.
 */
@Entity
@Table(
    name = "safe_routes",
    indexes = {
        @Index(name = "idx_safe_route_id", columnList = "safe_route_id", unique = true),
        @Index(name = "idx_safe_route_coords", columnList = "origin_latitude,origin_longitude,destination_latitude,destination_longitude"),
        @Index(name = "idx_safe_route_score", columnList = "safety_score"),
        @Index(name = "idx_safe_route_created", columnList = "created_at")
    }
)
public class SafeRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "safe_route_id", nullable = false, unique = true, length = 64)
    private String safeRouteId;

    @Column(name = "origin_latitude", nullable = false)
    private Double originLatitude;

    @Column(name = "origin_longitude", nullable = false)
    private Double originLongitude;

    @Column(name = "destination_latitude", nullable = false)
    private Double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private Double destinationLongitude;

    @Column(name = "origin_address", length = 255)
    private String originAddress;

    @Column(name = "destination_address", length = 255)
    private String destinationAddress;

    @Column(name = "travel_mode", nullable = false, length = 20)
    private String travelMode = "WALK";

    @Column(name = "distance_meters")
    private Integer distanceMeters = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "safety_score", nullable = false)
    private Integer safetyScore = 70;

    @Column(name = "safety_level", nullable = false, length = 30)
    private String safetyLevel = "SAFE";

    @Column(name = "data_confidence", nullable = false, length = 20)
    private String dataConfidence = "MEDIUM";

    @Column(name = "encoded_polyline", columnDefinition = "TEXT")
    private String encodedPolyline;

    @Column(name = "route_description", length = 255)
    private String routeDescription;

    @Column(name = "route_summary", columnDefinition = "TEXT")
    private String routeSummary;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Column(name = "auto_grievance_filed", nullable = false)
    private Boolean autoGrievanceFiled = false;

    @Column(name = "auto_grievance_tracking_id", length = 64)
    private String autoGrievanceTrackingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SafeRoute() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SafeRoute(String safeRouteId, Double originLatitude, Double originLongitude,
                     Double destinationLatitude, Double destinationLongitude,
                     String originAddress, String destinationAddress, String travelMode,
                     Integer distanceMeters, Integer durationSeconds, Integer safetyScore,
                     String safetyLevel, String dataConfidence, String encodedPolyline,
                     String routeDescription) {
        this.safeRouteId = safeRouteId;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.travelMode = (travelMode != null && !travelMode.isBlank()) ? travelMode : "WALK";
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.safetyScore = safetyScore != null ? safetyScore : 70;
        this.safetyLevel = safetyLevel != null ? safetyLevel : "SAFE";
        this.dataConfidence = dataConfidence != null ? dataConfidence : "MEDIUM";
        this.encodedPolyline = encodedPolyline;
        this.routeDescription = routeDescription;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.autoGrievanceFiled == null) {
            this.autoGrievanceFiled = false;
        }
        if (this.averageRating == null) {
            this.averageRating = 0.0;
        }
        if (this.ratingCount == null) {
            this.ratingCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSafeRouteId() {
        return safeRouteId;
    }

    public void setSafeRouteId(String safeRouteId) {
        this.safeRouteId = safeRouteId;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public void setOriginLatitude(Double originLatitude) {
        this.originLatitude = originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public void setOriginLongitude(Double originLongitude) {
        this.originLongitude = originLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getOriginAddress() {
        return originAddress;
    }

    public void setOriginAddress(String originAddress) {
        this.originAddress = originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Integer safetyScore) {
        this.safetyScore = safetyScore;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(String safetyLevel) {
        this.safetyLevel = safetyLevel;
    }

    public String getDataConfidence() {
        return dataConfidence;
    }

    public void setDataConfidence(String dataConfidence) {
        this.dataConfidence = dataConfidence;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    public String getRouteDescription() {
        return routeDescription;
    }

    public void setRouteDescription(String routeDescription) {
        this.routeDescription = routeDescription;
    }

    public String getRouteSummary() {
        return routeSummary;
    }

    public void setRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Boolean getAutoGrievanceFiled() {
        return autoGrievanceFiled;
    }

    public void setAutoGrievanceFiled(Boolean autoGrievanceFiled) {
        this.autoGrievanceFiled = autoGrievanceFiled;
    }

    public String getAutoGrievanceTrackingId() {
        return autoGrievanceTrackingId;
    }

    public void setAutoGrievanceTrackingId(String autoGrievanceTrackingId) {
        this.autoGrievanceTrackingId = autoGrievanceTrackingId;
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
