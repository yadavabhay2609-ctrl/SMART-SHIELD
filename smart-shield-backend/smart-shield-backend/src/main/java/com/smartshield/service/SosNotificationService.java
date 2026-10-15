package com.smartshield.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.smartshield.model.EmergencyContact;
import com.smartshield.model.Grievance;
import com.smartshield.model.GrievanceStatus;
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

    /**
     * Sends grievance submission confirmation email to citizen.
     */
    public boolean sendGrievanceConfirmation(Grievance grievance) {
        if (!isMailConfigured() || grievance == null || grievance.getCitizen() == null) {
            return false;
        }

        String toEmail = grievance.getCitizen().getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }

        try {
            String citizenName = grievance.getCitizen().getFullName() != null
                    ? grievance.getCitizen().getFullName()
                    : "Citizen";
            String trackingId = grievance.getTrackingId() != null ? grievance.getTrackingId() : "N/A";

            String subject = "Smart Shield: Grievance Registration Confirmed [" + trackingId + "]";
            StringBuilder body = new StringBuilder();
            body.append("Dear ").append(citizenName).append(",\n\n");
            body.append("Your public grievance has been successfully registered on Smart Shield.\n\n");
            body.append("GRIEVANCE DETAILS:\n");
            body.append("• Tracking ID: ").append(trackingId).append("\n");
            body.append("• Category: ").append(grievance.getCategory() != null ? grievance.getCategory().name() : "GENERAL").append("\n");
            body.append("• Priority: ").append(grievance.getPriority() != null ? grievance.getPriority().name() : "MEDIUM").append("\n");
            body.append("• Title: ").append(grievance.getTitle() != null ? grievance.getTitle().trim() : "N/A").append("\n");
            body.append("• Location: ").append(grievance.getLocation() != null ? grievance.getLocation().trim() : "N/A").append("\n");
            body.append("• Status: PENDING\n\n");
            body.append("You can track status updates and administrative progress anytime using your Tracking ID on the Smart Shield Citizen Portal.\n\n");
            body.append("Regards,\nSmart Shield Public Grievance Administration");

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(toEmail.trim());
            msg.setSubject(subject);
            msg.setText(body.toString());

            mailSender.send(msg);
            log.info("Grievance confirmation email sent to citizen {}", toEmail);
            return true;
        } catch (MailException e) {
            log.warn("Failed to dispatch grievance confirmation email: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Safe grievance email error: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Sends grievance status update email to citizen.
     */
    public boolean sendGrievanceStatusUpdate(Grievance grievance,
                                            GrievanceStatus oldStatus,
                                            GrievanceStatus newStatus,
                                            String remarks) {
        if (!isMailConfigured() || grievance == null || grievance.getCitizen() == null) {
            return false;
        }

        String toEmail = grievance.getCitizen().getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }

        try {
            String citizenName = grievance.getCitizen().getFullName() != null
                    ? grievance.getCitizen().getFullName()
                    : "Citizen";
            String trackingId = grievance.getTrackingId() != null ? grievance.getTrackingId() : "N/A";

            String subject = "Smart Shield: Status Update for Grievance [" + trackingId + "]";
            StringBuilder body = new StringBuilder();
            body.append("Dear ").append(citizenName).append(",\n\n");
            body.append("The status of your registered grievance has been updated by municipal administration.\n\n");
            body.append("UPDATE SUMMARY:\n");
            body.append("• Tracking ID: ").append(trackingId).append("\n");
            body.append("• Previous Status: ").append(oldStatus != null ? oldStatus.name() : "UNKNOWN").append("\n");
            body.append("• Current Status: ").append(newStatus != null ? newStatus.name() : "UPDATED").append("\n");
            body.append("• Category: ").append(grievance.getCategory() != null ? grievance.getCategory().name() : "GENERAL").append("\n");
            if (remarks != null && !remarks.isBlank()) {
                body.append("• Remarks: ").append(remarks.trim()).append("\n");
            }
            body.append("\nTrack your grievance status history anytime on the Smart Shield Citizen Portal.\n\n");
            body.append("Regards,\nSmart Shield Public Grievance Administration");

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(toEmail.trim());
            msg.setSubject(subject);
            msg.setText(body.toString());

            mailSender.send(msg);
            log.info("Grievance status update email sent to citizen {}", toEmail);
            return true;
        } catch (MailException e) {
            log.warn("Failed to dispatch grievance status update email: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Safe grievance status email error: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Dispatches a safe integration test email without sending real citizen notifications.
     */
    public boolean testEmailConfiguration(String testRecipient) {
        if (!isMailConfigured()) {
            log.info("Email test skipped: SMTP is not configured.");
            return false;
        }

        if (testRecipient == null || testRecipient.isBlank()) {
            log.warn("Email test skipped: No recipient provided.");
            return false;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(testRecipient.trim());
            msg.setSubject("Smart Shield: SMTP Integration Test");
            msg.setText("This is a test notification confirming that the Smart Shield SMTP email service is operational.");

            mailSender.send(msg);
            log.info("Test email successfully sent to {}", testRecipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to dispatch test email safely: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
