package com.smartshield.dto;

public class PublicConfigResponse {

    private String mapsApiKey;
    private boolean mapsConfigured;

    public PublicConfigResponse() {
    }

    public PublicConfigResponse(String mapsApiKey, boolean mapsConfigured) {
        this.mapsApiKey = mapsApiKey;
        this.mapsConfigured = mapsConfigured;
    }

    public String getMapsApiKey() {
        return mapsApiKey;
    }

    public void setMapsApiKey(String mapsApiKey) {
        this.mapsApiKey = mapsApiKey;
    }

    public boolean isMapsConfigured() {
        return mapsConfigured;
    }

    public void setMapsConfigured(boolean mapsConfigured) {
        this.mapsConfigured = mapsConfigured;
    }
}
