package com.smartshield.dto;

import com.smartshield.model.GrievanceCategory;
import com.smartshield.model.GrievancePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateGrievanceRequest {

    @NotNull(message = "Category is required")
    private GrievanceCategory category;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String location;

    private GrievancePriority priority;

    public CreateGrievanceRequest() {
    }

    public CreateGrievanceRequest(GrievanceCategory category, String title, String description,
                                  String location, GrievancePriority priority) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
        this.priority = priority;
    }

    public GrievanceCategory getCategory() {
        return category;
    }

    public void setCategory(GrievanceCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public GrievancePriority getPriority() {
        return priority;
    }

    public void setPriority(GrievancePriority priority) {
        this.priority = priority;
    }
}
