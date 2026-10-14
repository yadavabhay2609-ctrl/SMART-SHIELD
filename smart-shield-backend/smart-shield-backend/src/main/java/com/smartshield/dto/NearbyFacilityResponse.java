package com.smartshield.dto;

public class NearbyFacilityResponse {

    private String name;
    private String type; // POLICE, HOSPITAL, EMERGENCY
    private Double latitude;
    private Double longitude;
    private Integer distanceMeters;

    public NearbyFacilityResponse() {
    }

    public NearbyFacilityResponse(String name, String type, Double latitude, Double longitude, Integer distanceMeters) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}
