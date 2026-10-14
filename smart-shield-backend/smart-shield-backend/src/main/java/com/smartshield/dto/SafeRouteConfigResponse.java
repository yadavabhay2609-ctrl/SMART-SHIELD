package com.smartshield.dto;

public class SafeRouteConfigResponse {

    private boolean googleRoutesConfigured;
    private boolean googlePlacesConfigured;
    private boolean googleMapsJsConfigured;
    private String mapsBrowserApiKey; // Browser-safe key only
    private String statusMessage;
    private boolean openStreetMapConfigured = true;
    private String routingProvider = "OpenStreetMap / OSRM";

    public SafeRouteConfigResponse() {
    }

    public SafeRouteConfigResponse(boolean googleRoutesConfigured, boolean googlePlacesConfigured,
                                   boolean googleMapsJsConfigured, String mapsBrowserApiKey,
                                   String statusMessage) {
        this.googleRoutesConfigured = googleRoutesConfigured;
        this.googlePlacesConfigured = googlePlacesConfigured;
        this.googleMapsJsConfigured = googleMapsJsConfigured;
        this.mapsBrowserApiKey = mapsBrowserApiKey;
        this.statusMessage = statusMessage;
    }

    public boolean isGoogleRoutesConfigured() {
        return googleRoutesConfigured;
    }

    public void setGoogleRoutesConfigured(boolean googleRoutesConfigured) {
        this.googleRoutesConfigured = googleRoutesConfigured;
    }

    public boolean isGooglePlacesConfigured() {
        return googlePlacesConfigured;
    }

    public void setGooglePlacesConfigured(boolean googlePlacesConfigured) {
        this.googlePlacesConfigured = googlePlacesConfigured;
    }

    public boolean isGoogleMapsJsConfigured() {
        return googleMapsJsConfigured;
    }

    public void setGoogleMapsJsConfigured(boolean googleMapsJsConfigured) {
        this.googleMapsJsConfigured = googleMapsJsConfigured;
    }

    public String getMapsBrowserApiKey() {
        return mapsBrowserApiKey;
    }

    public void setMapsBrowserApiKey(String mapsBrowserApiKey) {
        this.mapsBrowserApiKey = mapsBrowserApiKey;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public boolean isOpenStreetMapConfigured() {
        return openStreetMapConfigured;
    }

    public void setOpenStreetMapConfigured(boolean openStreetMapConfigured) {
        this.openStreetMapConfigured = openStreetMapConfigured;
    }

    public String getRoutingProvider() {
        return routingProvider;
    }

    public void setRoutingProvider(String routingProvider) {
        this.routingProvider = routingProvider;
    }
}
