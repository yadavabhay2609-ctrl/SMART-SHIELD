package com.smartshield.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.smartshield.model.EmergencyContact;
import com.smartshield.model.SosAlert;
import com.smartshield.model.SosNotificationStatus;

/**
 * Resilient notification service for SOS emergency alerts.
 * Optional mail support: if credentials are not configured,
 * the alert is marked honestly as NOT_CONFIGURED without throwing exceptions or blocking persistence.
 */
@Service
public class SosNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SosNotificationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.from:noreply@smartshield.gov.in}")
    private String mailFrom;

    public SosNotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public static class NotificationResult {
        private final SosNotificationStatus status;
        private final String details;
        private final int recipientsCount;

        public NotificationResult(SosNotificationStatus status, String details, int recipientsCount) {
            this.status = status;
            this.details = details;
            this.recipientsCount = recipientsCount;
        }

        public SosNotificationStatus getStatus() {
            return status;
        }

        public String getDetails() {
            return details;
        }

        public int getRecipientsCount() {
            return recipientsCount;
        }
    }

    /**
     * Checks whether an external mail transport is configured.
     */
    public boolean isMailConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank() && mailUsername != null && !mailUsername.isBlank();
    }

    /**
     * Attempts to notify active emergency contacts via email if configured.
     * Never fails or throws exception outwards to ensure SOS alert persistence is not interrupted.
     */
    public NotificationResult notifyContacts(SosAlert alert, List<EmergencyContact> contacts) {
        if (!isMailConfigured()) {
            String details = "Email notification service is not configured on this server. SOS alert registered safely.";
            log.info("SOS Alert ID {} created. {}", alert.getId(), details);
            return new NotificationResult(SosNotificationStatus.NOT_CONFIGURED, details, 0);
        }

        if (contacts == null || contacts.isEmpty()) {
            String details = "Citizen has no registered active emergency contacts.";
            log.info("SOS Alert ID {}: {}", alert.getId(), details);
            return new NotificationResult(SosNotificationStatus.NOT_CONFIGURED, details, 0);
        }

        List<EmergencyContact> recipients = contacts.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isBlank())
                .toList();

        if (recipients.isEmpty()) {
            String details = "None of the citizen's active emergency contacts have an email address configured.";
            log.info("SOS Alert ID {}: {}", alert.getId(), details);
            return new NotificationResult(SosNotificationStatus.NOT_CONFIGURED, details, 0);
        }

        String citizenName = alert.getCitizen() != null ? alert.getCitizen().getFullName() : "A Smart Shield Citizen";
        String triggerTime = alert.getTriggeredAt() != null ? alert.getTriggeredAt().format(TIME_FORMATTER) : "Just now";

        String subject = "URGENT: Smart Shield Emergency SOS Alert for " + citizenName;
        StringBuilder body = new StringBuilder();
        body.append("====================================================\n");
        body.append("        SMART SHIELD EMERGENCY SOS NOTIFICATION\n");
        body.append("====================================================\n\n");
        body.append("This is an automated emergency alert dispatched by Smart Shield.\n\n");
        body.append("CITIZEN DETAILS:\n");
        body.append("• Name: ").append(citizenName).append("\n");
        body.append("• SOS ID: #").append(alert.getId() != null ? alert.getId() : "N/A").append("\n");
        body.append("• Triggered At: ").append(triggerTime).append("\n\n");
        body.append("EMERGENCY LOCATION:\n");
        body.append("• Latitude: ").append(alert.getLatitude()).append("\n");
        body.append("• Longitude: ").append(alert.getLongitude()).append("\n");
        if (alert.getAccuracy() != null) {
            body.append("• Approximate Accuracy: ±").append(alert.getAccuracy()).append(" meters\n");
        }
        body.append("• Map Coordinates Link: https://www.google.com/maps?q=")
            .append(alert.getLatitude()).append(",").append(alert.getLongitude()).append("\n\n");
        body.append("CITIZEN MESSAGE:\n");
        body.append("\"").append(alert.getMessage()).append("\"\n\n");
        body.append("RECOMMENDED ACTIONS:\n");
        body.append("1. Try reaching the citizen immediately by phone.\n");
        body.append("2. If in immediate danger, contact emergency services:\n");
        body.append("   • National Emergency Number: 112\n");
        body.append("   • Women Safety Helpline: 1091\n");
        body.append("   • Police Emergency: 100\n\n");
        body.append("----------------------------------------------------\n");
        body.append("Smart Shield • Citizen Protection & Safety Platform\n");

        int sentCount = 0;
        int failCount = 0;

        for (EmergencyContact recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(recipient.getEmail().trim());
                message.setSubject(subject);
                message.setText(body.toString());

                mailSender.send(message);
                sentCount++;
                log.info("SOS Alert notification successfully dispatched to {}", recipient.getEmail());
            } catch (Exception e) {
                failCount++;
                log.error("Failed to dispatch SOS alert email to recipient: {}", e.getMessage());
            }
        }

        if (sentCount > 0 && failCount == 0) {
            return new NotificationResult(SosNotificationStatus.SENT, "Emergency alerts successfully emailed to " + sentCount + " contact(s).", sentCount);
        } else if (sentCount > 0) {
            return new NotificationResult(SosNotificationStatus.SENT, "Alerts sent to " + sentCount + " contact(s); failed for " + failCount + " contact(s).", sentCount);
        } else {
            return new NotificationResult(SosNotificationStatus.FAILED, "Failed to deliver email notifications to configured recipients.", 0);
        }
    }
}
