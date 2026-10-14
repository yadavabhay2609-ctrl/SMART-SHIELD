package com.smartshield.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationResolutionResponse {

    private boolean resolved;
    private String city;
    private String state;
    private String country;
    private String formattedAddress;
    private String provider; // OFFLINE_VERIFIED_BOUNDARIES or GOOGLE_GEOCODING
    private Double latitude;
    private Double longitude;
    private List<DestinationSuggestionDto> nearbyDestinations = new ArrayList<>();

    public LocationResolutionResponse() {
    }

    public LocationResolutionResponse(boolean resolved, String city, String state, String country,
                                      String formattedAddress, String provider,
                                      Double latitude, Double longitude,
                                      List<DestinationSuggestionDto> nearbyDestinations) {
        this.resolved = resolved;
        this.city = city;
        this.state = state;
        this.country = country;
        this.formattedAddress = formattedAddress;
        this.provider = provider;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nearbyDestinations = (nearbyDestinations != null) ? nearbyDestinations : new ArrayList<>();
    }

    public static LocationResolutionResponse unresolved(Double lat, Double lng) {
        return new LocationResolutionResponse(
                false,
                null,
                null,
                null,
                "Current location detected",
                "NONE",
                lat,
                lng,
                new ArrayList<>()
        );
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public List<DestinationSuggestionDto> getNearbyDestinations() {
        return nearbyDestinations;
    }

    public void setNearbyDestinations(List<DestinationSuggestionDto> nearbyDestinations) {
        this.nearbyDestinations = (nearbyDestinations != null) ? nearbyDestinations : new ArrayList<>();
    }
}
