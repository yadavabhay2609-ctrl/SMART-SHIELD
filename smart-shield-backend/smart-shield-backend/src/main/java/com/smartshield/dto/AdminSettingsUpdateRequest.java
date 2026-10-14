package com.smartshield.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AdminSettingsUpdateRequest {

    @NotNull(message = "auto_file_enabled is required")
    @JsonProperty("auto_file_enabled")
    @JsonAlias("autoFileEnabled")
    private Boolean auto_file_enabled;

    @NotNull(message = "auto_file_threshold is required")
    @Min(value = 1, message = "auto_file_threshold must be at least 1")
    @Max(value = 10, message = "auto_file_threshold cannot exceed 10")
    @JsonProperty("auto_file_threshold")
    @JsonAlias("autoFileThreshold")
    private Integer auto_file_threshold;

    @NotNull(message = "min_ratings_for_auto_file is required")
    @Min(value = 1, message = "min_ratings_for_auto_file must be at least 1")
    @JsonProperty("min_ratings_for_auto_file")
    @JsonAlias("minRatingsForAutoFile")
    private Integer min_ratings_for_auto_file;

    public AdminSettingsUpdateRequest() {
    }

    public AdminSettingsUpdateRequest(Boolean auto_file_enabled, Integer auto_file_threshold, Integer min_ratings_for_auto_file) {
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
