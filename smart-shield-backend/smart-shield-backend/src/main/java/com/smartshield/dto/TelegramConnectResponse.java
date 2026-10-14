package com.smartshield.dto;

import java.time.LocalDateTime;

public class TelegramConnectResponse {

    private Long contactId;
    private String contactName;
    private String connectionCode;
    private String botUsername;
    private String botDeepLink;
    private LocalDateTime expiresAt;
    private boolean telegramConnected;
    private String message;

    public TelegramConnectResponse() {
    }

    public TelegramConnectResponse(Long contactId, String contactName, String connectionCode,
                                   String botUsername, String botDeepLink, LocalDateTime expiresAt,
                                   boolean telegramConnected, String message) {
        this.contactId = contactId;
        this.contactName = contactName;
        this.connectionCode = connectionCode;
        this.botUsername = botUsername;
        this.botDeepLink = botDeepLink;
        this.expiresAt = expiresAt;
        this.telegramConnected = telegramConnected;
        this.message = message;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getConnectionCode() {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode) {
        this.connectionCode = connectionCode;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public String getBotDeepLink() {
        return botDeepLink;
    }

    public void setBotDeepLink(String botDeepLink) {
        this.botDeepLink = botDeepLink;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isTelegramConnected() {
        return telegramConnected;
    }

    public void setTelegramConnected(boolean telegramConnected) {
        this.telegramConnected = telegramConnected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
