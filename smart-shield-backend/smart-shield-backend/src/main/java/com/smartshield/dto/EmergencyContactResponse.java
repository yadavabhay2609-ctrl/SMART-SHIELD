package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.EmergencyContact;

public class EmergencyContactResponse {

    private Long id;
    private String name;
    private String phoneNumber;
    private String relationship;
    private String email;
    private boolean active;
    private boolean telegramConnected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyContactResponse() {
    }

    public EmergencyContactResponse(Long id, String name, String phoneNumber, String relationship,
                                  String email, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, phoneNumber, relationship, email, active, false, createdAt, updatedAt);
    }

    public EmergencyContactResponse(Long id, String name, String phoneNumber, String relationship,
                                  String email, boolean active, boolean telegramConnected,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.email = email;
        this.active = active;
        this.telegramConnected = telegramConnected;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmergencyContactResponse fromEntity(EmergencyContact contact) {
        if (contact == null) {
            return null;
        }
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getRelationship(),
                contact.getEmail(),
                contact.isActive(),
                contact.isTelegramConnected(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isTelegramConnected() {
        return telegramConnected;
    }

    public void setTelegramConnected(boolean telegramConnected) {
        this.telegramConnected = telegramConnected;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
