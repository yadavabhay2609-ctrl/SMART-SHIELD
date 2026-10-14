package com.smartshield.dto;

public class AuthResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String message;
    private String token;

    public AuthResponse(Long id, String fullName, String email, String role, String message) {
        this(id, fullName, email, role, message, null);
    }

    public AuthResponse(Long id, String fullName, String email, String role, String message, String token) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.message = message;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}