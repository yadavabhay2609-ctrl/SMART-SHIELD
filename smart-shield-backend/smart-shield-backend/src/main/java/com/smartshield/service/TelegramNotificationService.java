package com.smartshield.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartshield.model.EmergencyContact;
import com.smartshield.model.SosAlert;
import com.smartshield.repository.EmergencyContactRepository;

/**
 * Resilient Telegram notification service for Smart Shield SOS alerts.
 * Operates strictly server-side and never exposes bot tokens to clients or logs.
 * If Telegram is not configured or bot token is missing, reports NOT_CONFIGURED honestly
 * without disrupting SOS persistence or throwing exceptions.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmergencyContactRepository emergencyContactRepository;
    private final HttpClient httpClient;

    @Value("${smartshield.telegram.enabled:false}")
    private boolean enabled;

    @Value("${smartshield.telegram.bot-token:}")
    private String botToken;

    @Value("${smartshield.telegram.bot-username:SmartShieldAlertBot}")
    private String botUsername;

    public TelegramNotificationService(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static class TelegramResult {
        private final String status; // SENT, NOT_CONNECTED, NOT_CONFIGURED, FAILED
        private final String details;
        private final int sentCount;

        public TelegramResult(String status, String details, int sentCount) {
            this.status = status;
            this.details = details;
            this.sentCount = sentCount;
        }

        public String getStatus() {
            return status;
        }

        public String getDetails() {
            return details;
        }

        public int getSentCount() {
            return sentCount;
        }
    }

    /**
     * Checks if Telegram notifications are enabled and configured with a bot token.
     */
    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank();
    }

    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Sends SOS emergency alert notifications to contacts who have connected their Telegram account.
     * Guaranteed never to throw exceptions to preserve SOS record persistence.
     */
    public TelegramResult sendSosNotification(SosAlert alert, List<EmergencyContact> contacts) {
        if (!isConfigured()) {
            String details = "Telegram notification service is not configured on this server.";
            log.info("SOS Alert ID {}: {}", alert.getId(), details);
            return new TelegramResult("NOT_CONFIGURED", details, 0);
        }

        if (contacts == null || contacts.isEmpty()) {
            String details = "Citizen has no registered emergency contacts.";
            log.info("SOS Alert ID {}: {}", alert.getId(), details);
            return new TelegramResult("NOT_CONNECTED", details, 0);
        }

        List<EmergencyContact> telegramRecipients = contacts.stream()
                .filter(c -> c.isActive() && c.isTelegramConnected() && c.getTelegramChatId() != null && !c.getTelegramChatId().isBlank())
                .toList();

        if (telegramRecipients.isEmpty()) {
            String details = "No emergency contacts have connected Telegram.";
            log.info("SOS Alert ID {}: {}", alert.getId(), details);
            return new TelegramResult("NOT_CONNECTED", details, 0);
        }

        String citizenName = (alert.getCitizen() != null && alert.getCitizen().getFullName() != null)
                ? alert.getCitizen().getFullName()
                : "A Smart Shield Citizen";
        String triggerTime = alert.getTriggeredAt() != null
                ? alert.getTriggeredAt().format(TIME_FORMATTER)
                : LocalDateTime.now().format(TIME_FORMATTER);

        String mapsUrl = "https://www.google.com/maps?q=" + alert.getLatitude() + "," + alert.getLongitude();

        StringBuilder msg = new StringBuilder();
        msg.append("🚨 SMART SHIELD SOS ALERT\n\n");
        msg.append("An emergency SOS has been triggered.\n\n");
        msg.append("Citizen: ").append(citizenName).append("\n");
        msg.append("SOS ID: #").append(alert.getId() != null ? alert.getId() : "N/A").append("\n");
        msg.append("Time: ").append(triggerTime).append("\n\n");
        msg.append("📍 Location:\n").append(mapsUrl).append("\n\n");
        if (alert.getMessage() != null && !alert.getMessage().isBlank()) {
            msg.append("Message: \"").append(alert.getMessage().trim()).append("\"\n\n");
        }
        msg.append("Please contact the person immediately.");

        int sentCount = 0;
        int failCount = 0;

        for (EmergencyContact contact : telegramRecipients) {
            boolean success = dispatchMessageToChat(contact.getTelegramChatId(), msg.toString());
            if (success) {
                sentCount++;
            } else {
                failCount++;
            }
        }

        if (sentCount > 0 && failCount == 0) {
            return new TelegramResult("SENT", "SOS alert delivered to " + sentCount + " Telegram contact(s).", sentCount);
        } else if (sentCount > 0) {
            return new TelegramResult("SENT", "SOS alert delivered to " + sentCount + " contact(s); failed for " + failCount + " contact(s).", sentCount);
        } else {
            return new TelegramResult("FAILED", "Failed to dispatch Telegram notifications to connected recipients.", 0);
        }
    }

    /**
     * Dispatches a text message to a specific Telegram chat ID.
     */
    private boolean dispatchMessageToChat(String chatId, String text) {
        if (!isConfigured() || chatId == null || chatId.isBlank()) {
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            String jsonPayload = "{\"chat_id\":\"" + escapeJson(chatId) + "\",\"text\":\"" + escapeJson(text) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Telegram message successfully sent to chat ID {}", maskChatId(chatId));
                return true;
            } else {
                log.warn("Telegram API returned non-200 status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to dispatch Telegram message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates and associates a Telegram chat ID with an emergency contact using a secure connection code.
     */
    @Transactional
    public boolean verifyAndLinkCode(String connectionCode, String chatId) {
        if (connectionCode == null || connectionCode.isBlank() || chatId == null || chatId.isBlank()) {
            return false;
        }

        String cleanCode = connectionCode.trim().toUpperCase();
        Optional<EmergencyContact> contactOpt = emergencyContactRepository.findByTelegramConnectionCode(cleanCode);
        if (contactOpt.isEmpty()) {
            return false;
        }

        EmergencyContact contact = contactOpt.get();
        if (contact.getTelegramCodeExpiresAt() == null || contact.getTelegramCodeExpiresAt().isBefore(LocalDateTime.now())) {
            // Expired code
            return false;
        }

        contact.setTelegramChatId(chatId.trim());
        contact.setTelegramConnected(true);
        contact.setTelegramConnectionCode(null);
        contact.setTelegramCodeExpiresAt(null);
        emergencyContactRepository.save(contact);

        log.info("Successfully linked Telegram chat {} to Emergency Contact ID {}", maskChatId(chatId), contact.getId());

        // Send confirmation greeting to the relative
        String greeting = "✓ You are now connected as an Emergency Contact for "
                + (contact.getUser() != null ? contact.getUser().getFullName() : "a Smart Shield user")
                + " on Smart Shield.\nYou will receive instant emergency alerts if they trigger SOS.";
        dispatchMessageToChat(chatId, greeting);

        return true;
    }

    /**
     * Checks recent Telegram bot updates for incoming messages containing the expected connection code.
     * Safely enables local development without requiring a public HTTPS webhook.
     */
    @Transactional
    public boolean checkUpdatesForCode(String expectedCode) {
        if (!isConfigured() || expectedCode == null || expectedCode.isBlank()) {
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?limit=25";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return false;
            }

            String body = response.body();
            String cleanExpected = expectedCode.trim().toUpperCase();

            // Match text containing the code or /start <code>
            if (body.contains(cleanExpected)) {
                // Parse chat_id associated with this message
                Pattern p = Pattern.compile("\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)[^}]*\\}[^}]*\"text\"\\s*:\\s*\"([^\"]*)\"");
                Matcher m = p.matcher(body);
                while (m.find()) {
                    String chatId = m.group(1);
                    String text = m.group(2).toUpperCase();
                    if (text.contains(cleanExpected)) {
                        return verifyAndLinkCode(cleanExpected, chatId);
                    }
                }

                // Fallback secondary regex if chat id comes after text
                Pattern p2 = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*" + Pattern.quote(cleanExpected) + "[^\"]*)\"[^}]*\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)");
                Matcher m2 = p2.matcher(body);
                if (m2.find()) {
                    String chatId = m2.group(2);
                    return verifyAndLinkCode(cleanExpected, chatId);
                }
            }
        } catch (Exception e) {
            log.warn("Telegram checkUpdates check failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Unlinks Telegram association for an emergency contact.
     */
    @Transactional
    public void unlinkContact(EmergencyContact contact) {
        if (contact != null) {
            contact.setTelegramChatId(null);
            contact.setTelegramConnected(false);
            contact.setTelegramConnectionCode(null);
            contact.setTelegramCodeExpiresAt(null);
            emergencyContactRepository.save(contact);
        }
    }

    private String maskChatId(String chatId) {
        if (chatId == null || chatId.length() <= 4) {
            return "****";
        }
        return "***" + chatId.substring(chatId.length() - 4);
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
