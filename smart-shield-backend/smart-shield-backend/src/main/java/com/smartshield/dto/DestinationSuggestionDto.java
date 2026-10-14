package com.smartshield.dto;

public class DestinationSuggestionDto {

    private String name;
    private String category; // POLICE, HOSPITAL, METRO, TRANSIT, GOVT, EDUCATION, COMMERCIAL, PUBLIC_PLACE
    private String address;
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;

    public DestinationSuggestionDto() {
    }

    public DestinationSuggestionDto(String name, String category, String address, String city, String state, Double latitude, Double longitude) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
}
