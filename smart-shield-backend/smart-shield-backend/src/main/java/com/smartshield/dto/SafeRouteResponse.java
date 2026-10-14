package com.smartshield.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.smartshield.model.SafeRoute;

public class SafeRouteResponse {

    private String safeRouteId;
    private Double originLatitude;
    private Double originLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String originAddress;
    private String destinationAddress;
    private String travelMode;
    private Integer distanceMeters;
    private Integer durationSeconds;
    private Integer safetyScore;
    private String safetyLevel;
    private String dataConfidence;
    private String encodedPolyline;
    private String routeDescription;
    private String routeSummary;
    private Double averageRating;
    private Integer ratingCount;
    private Boolean autoGrievanceFiled;
    private String autoGrievanceTrackingId;
    private List<SafetyFactorResponse> factors = new ArrayList<>();
    private List<NearbyFacilityResponse> nearbyFacilities = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private LocalDateTime createdAt;

    public SafeRouteResponse() {
    }

    public static SafeRouteResponse fromEntity(SafeRoute entity) {
        if (entity == null) {
            return null;
        }
        SafeRouteResponse response = new SafeRouteResponse();
        response.setSafeRouteId(entity.getSafeRouteId());
        response.setOriginLatitude(entity.getOriginLatitude());
        response.setOriginLongitude(entity.getOriginLongitude());
        response.setDestinationLatitude(entity.getDestinationLatitude());
        response.setDestinationLongitude(entity.getDestinationLongitude());
        response.setOriginAddress(entity.getOriginAddress());
        response.setDestinationAddress(entity.getDestinationAddress());
        response.setTravelMode(entity.getTravelMode());
        response.setDistanceMeters(entity.getDistanceMeters());
        response.setDurationSeconds(entity.getDurationSeconds());
        response.setSafetyScore(entity.getSafetyScore());
        response.setSafetyLevel(entity.getSafetyLevel());
        response.setDataConfidence(entity.getDataConfidence());
        response.setEncodedPolyline(entity.getEncodedPolyline());
        response.setRouteDescription(entity.getRouteDescription());
        response.setRouteSummary(entity.getRouteSummary());
        response.setAverageRating(entity.getAverageRating());
        response.setRatingCount(entity.getRatingCount());
        response.setAutoGrievanceFiled(entity.getAutoGrievanceFiled());
        response.setAutoGrievanceTrackingId(entity.getAutoGrievanceTrackingId());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
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

    public List<SafetyFactorResponse> getFactors() {
        return factors;
    }

    public void setFactors(List<SafetyFactorResponse> factors) {
        this.factors = (factors != null) ? factors : new ArrayList<>();
    }

    public List<NearbyFacilityResponse> getNearbyFacilities() {
        return nearbyFacilities;
    }

    public void setNearbyFacilities(List<NearbyFacilityResponse> nearbyFacilities) {
        this.nearbyFacilities = (nearbyFacilities != null) ? nearbyFacilities : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = (warnings != null) ? warnings : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
