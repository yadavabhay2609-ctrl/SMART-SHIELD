package com.smartshield.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Citizen-owned emergency contact entity.
 * Belongs exclusively to the authenticated citizen (User).
 */
@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "telegram_chat_id", length = 50)
    private String telegramChatId;

    @Column(name = "telegram_connected", nullable = false)
    private boolean telegramConnected = false;

    @Column(name = "telegram_connection_code", length = 30)
    private String telegramConnectionCode;

    @Column(name = "telegram_code_expires_at")
    private LocalDateTime telegramCodeExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public EmergencyContact() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }

    public EmergencyContact(User user, String name, String phoneNumber, String relationship, String email) {
        this.user = user;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.email = email;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isTelegramConnected() {
        return telegramConnected;
    }

    public void setTelegramConnected(boolean telegramConnected) {
        this.telegramConnected = telegramConnected;
    }

    public String getTelegramConnectionCode() {
        return telegramConnectionCode;
    }

    public void setTelegramConnectionCode(String telegramConnectionCode) {
        this.telegramConnectionCode = telegramConnectionCode;
    }

    public LocalDateTime getTelegramCodeExpiresAt() {
        return telegramCodeExpiresAt;
    }

    public void setTelegramCodeExpiresAt(LocalDateTime telegramCodeExpiresAt) {
        this.telegramCodeExpiresAt = telegramCodeExpiresAt;
    }
}
