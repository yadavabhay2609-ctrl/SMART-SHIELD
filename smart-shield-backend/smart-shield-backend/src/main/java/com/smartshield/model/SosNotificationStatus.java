package com.smartshield.model;

/**
 * Honest notification delivery statuses.
 * NEVER fake success: if email is not configured, NOT_CONFIGURED is recorded.
 */
public enum SosNotificationStatus {
    NOT_CONFIGURED,
    PENDING,
    SENT,
    FAILED
}
