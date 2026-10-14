package com.smartshield.dto;

public class AdminAuthResponse {

    private String adminId;
    private String role;
    private String message;
    private String token;

    public AdminAuthResponse() {
    }

    public AdminAuthResponse(String adminId, String role, String message, String token) {
        this.adminId = adminId;
        this.role = role;
        this.message = message;
        this.token = token;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
