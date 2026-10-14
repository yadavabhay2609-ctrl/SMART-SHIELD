package com.smartshield.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminSettingsResponse {

    @JsonProperty("auto_file_enabled")
    @JsonAlias("autoFileEnabled")
    private Boolean auto_file_enabled;

    @JsonProperty("auto_file_threshold")
    @JsonAlias("autoFileThreshold")
    private Integer auto_file_threshold;

    @JsonProperty("min_ratings_for_auto_file")
    @JsonAlias("minRatingsForAutoFile")
    private Integer min_ratings_for_auto_file;

    public AdminSettingsResponse() {
    }

    public AdminSettingsResponse(Boolean auto_file_enabled, Integer auto_file_threshold, Integer min_ratings_for_auto_file) {
        this.auto_file_enabled = auto_file_enabled;
        this.auto_file_threshold = auto_file_threshold;
        this.min_ratings_for_auto_file = min_ratings_for_auto_file;
    }

    public Boolean getAuto_file_enabled() {
        return auto_file_enabled;
    }

    public void setAuto_file_enabled(Boolean auto_file_enabled) {
        this.auto_file_enabled = auto_file_enabled;
    }

    public Integer getAuto_file_threshold() {
        return auto_file_threshold;
    }

    public void setAuto_file_threshold(Integer auto_file_threshold) {
        this.auto_file_threshold = auto_file_threshold;
    }

    public Integer getMin_ratings_for_auto_file() {
        return min_ratings_for_auto_file;
    }

    public void setMin_ratings_for_auto_file(Integer min_ratings_for_auto_file) {
        this.min_ratings_for_auto_file = min_ratings_for_auto_file;
    }
}
