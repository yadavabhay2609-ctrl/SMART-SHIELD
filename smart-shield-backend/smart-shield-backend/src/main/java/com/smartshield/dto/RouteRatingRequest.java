package com.smartshield.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RouteRatingRequest {

    @NotNull(message = "Rating value is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;

    @Size(max = 50, message = "Safety tag cannot exceed 50 characters")
    private String safetyTag;

    public RouteRatingRequest() {
    }

    public RouteRatingRequest(Integer rating, String comment, String safetyTag) {
        this.rating = rating;
        this.comment = comment;
        this.safetyTag = safetyTag;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSafetyTag() {
        return safetyTag;
    }

    public void setSafetyTag(String safetyTag) {
        this.safetyTag = safetyTag;
    }
}
