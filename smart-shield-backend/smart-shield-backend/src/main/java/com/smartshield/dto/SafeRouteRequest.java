package com.smartshield.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SafeRouteRequest {

    @NotNull(message = "Origin latitude is required")
    @DecimalMin(value = "-90.0", message = "Origin latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Origin latitude must be between -90 and 90")
    private Double originLatitude;

    @NotNull(message = "Origin longitude is required")
    @DecimalMin(value = "-180.0", message = "Origin longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Origin longitude must be between -180 and 180")
    private Double originLongitude;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin(value = "-90.0", message = "Destination latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Destination latitude must be between -90 and 90")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin(value = "-180.0", message = "Destination longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Destination longitude must be between -180 and 180")
    private Double destinationLongitude;

    private String originAddress;

    private String destinationAddress;

    private Integer distanceMeters;

    private Integer durationSeconds;

    private String encodedPolyline;

    @Pattern(regexp = "^(WALK|DRIVE)$", message = "Travel mode must be either WALK or DRIVE")
    private String travelMode = "WALK";

    public SafeRouteRequest() {
    }

    public SafeRouteRequest(Double originLatitude, Double originLongitude,
                            Double destinationLatitude, Double destinationLongitude,
                            String originAddress, String destinationAddress, String travelMode) {
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.travelMode = (travelMode != null && !travelMode.isBlank()) ? travelMode : "WALK";
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

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }
}
