package com.smartshield.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.EmergencyContactRequest;
import com.smartshield.dto.EmergencyContactResponse;
import com.smartshield.dto.TelegramConnectResponse;
import com.smartshield.model.EmergencyContact;
import com.smartshield.model.User;
import com.smartshield.repository.EmergencyContactRepository;
import com.smartshield.repository.UserRepository;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository,
                                  UserRepository userRepository,
                                  TelegramNotificationService telegramNotificationService) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.userRepository = userRepository;
        this.telegramNotificationService = telegramNotificationService;
    }

    /**
     * Lists all emergency contacts owned by the authenticated citizen.
     */
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getContactsForCitizen(Long citizenId) {
        return emergencyContactRepository.findByUserIdOrderByCreatedAtDesc(citizenId)
                .stream()
                .map(EmergencyContactResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new emergency contact for the authenticated citizen.
     */
    @Transactional
    public EmergencyContactResponse createContact(Long citizenId, EmergencyContactRequest request) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen user not found"));

        // Clean phone number for consistency
        String cleanPhone = request.getPhoneNumber().trim();

        EmergencyContact contact = new EmergencyContact(
                citizen,
                request.getName().trim(),
                cleanPhone,
                request.getRelationship().trim(),
                request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null
        );

        if (request.getActive() != null) {
            contact.setActive(request.getActive());
        }

        EmergencyContact saved = emergencyContactRepository.save(contact);
        return EmergencyContactResponse.fromEntity(saved);
    }

    /**
     * Updates an existing emergency contact with strict ownership verification.
     */
    @Transactional
    public EmergencyContactResponse updateContact(Long citizenId, Long contactId, EmergencyContactRequest request) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        // Strict ownership check: prevent Citizen A from modifying Citizen B's contact
        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        contact.setName(request.getName().trim());
        contact.setPhoneNumber(request.getPhoneNumber().trim());
        contact.setRelationship(request.getRelationship().trim());
        contact.setEmail(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null);

        if (request.getActive() != null) {
            contact.setActive(request.getActive());
        }

        EmergencyContact updated = emergencyContactRepository.save(contact);
        return EmergencyContactResponse.fromEntity(updated);
    }

    /**
     * Deletes an emergency contact with strict ownership verification.
     */
    @Transactional
    public void deleteContact(Long citizenId, Long contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        emergencyContactRepository.delete(contact);
    }

    /**
     * Generates a short-lived random single-use connection code for Telegram linking.
     */
    @Transactional
    public TelegramConnectResponse generateTelegramConnectCode(Long citizenId, Long contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        if (contact.isTelegramConnected()) {
            return new TelegramConnectResponse(
                    contact.getId(),
                    contact.getName(),
                    null,
                    telegramNotificationService.getBotUsername(),
                    null,
                    null,
                    true,
                    "Telegram is already connected for this emergency contact."
            );
        }

        String code = generateSecureCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        contact.setTelegramConnectionCode(code);
        contact.setTelegramCodeExpiresAt(expiresAt);
        emergencyContactRepository.save(contact);

        String botUser = telegramNotificationService.getBotUsername();
        String deepLink = (botUser != null && !botUser.isBlank())
                ? "https://t.me/" + botUser + "?start=" + code
                : null;

        return new TelegramConnectResponse(
                contact.getId(),
                contact.getName(),
                code,
                botUser,
                deepLink,
                expiresAt,
                false,
                "Ask your contact to open the Smart Shield Telegram bot and submit code " + code + " within 10 minutes."
        );
    }

    /**
     * Retrieves current Telegram status for an emergency contact, checking updates if an active code exists.
     */
    @Transactional
    public TelegramConnectResponse getTelegramStatus(Long citizenId, Long contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        if (contact.isTelegramConnected()) {
            return new TelegramConnectResponse(
                    contact.getId(),
                    contact.getName(),
                    null,
                    telegramNotificationService.getBotUsername(),
                    null,
                    null,
                    true,
                    "Telegram is connected and active for emergency broadcasts."
            );
        }

        // If contact has a pending code, attempt to check Telegram updates
        if (contact.getTelegramConnectionCode() != null && contact.getTelegramCodeExpiresAt() != null) {
            if (contact.getTelegramCodeExpiresAt().isAfter(LocalDateTime.now())) {
                boolean linked = telegramNotificationService.checkUpdatesForCode(contact.getTelegramConnectionCode());
                if (linked) {
                    contact = emergencyContactRepository.findById(contactId).orElse(contact);
                    if (contact.isTelegramConnected()) {
                        return new TelegramConnectResponse(
                                contact.getId(),
                                contact.getName(),
                                null,
                                telegramNotificationService.getBotUsername(),
                                null,
                                null,
                                true,
                                "Telegram was just successfully linked!"
                        );
                    }
                }

                String botUser = telegramNotificationService.getBotUsername();
                String deepLink = (botUser != null && !botUser.isBlank())
                        ? "https://t.me/" + botUser + "?start=" + contact.getTelegramConnectionCode()
                        : null;

                return new TelegramConnectResponse(
                        contact.getId(),
                        contact.getName(),
                        contact.getTelegramConnectionCode(),
                        botUser,
                        deepLink,
                        contact.getTelegramCodeExpiresAt(),
                        false,
                        "Pending connection: waiting for contact to activate the bot with code."
                );
            } else {
                contact.setTelegramConnectionCode(null);
                contact.setTelegramCodeExpiresAt(null);
                emergencyContactRepository.save(contact);
            }
        }

        return new TelegramConnectResponse(
                contact.getId(),
                contact.getName(),
                null,
                telegramNotificationService.getBotUsername(),
                null,
                null,
                false,
                "Telegram is not connected. Click 'Connect Telegram' to generate a connection code."
        );
    }

    /**
     * Explicit verification and manual linking method.
     */
    @Transactional
    public TelegramConnectResponse verifyAndLinkTelegram(Long citizenId, Long contactId, String chatId, String code) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        if (code == null || code.isBlank() || !code.trim().equalsIgnoreCase(contact.getTelegramConnectionCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or mismatched connection code.");
        }

        if (contact.getTelegramCodeExpiresAt() == null || contact.getTelegramCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection code has expired. Please generate a new code.");
        }

        if (chatId == null || chatId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telegram chat ID is required.");
        }

        contact.setTelegramChatId(chatId.trim());
        contact.setTelegramConnected(true);
        contact.setTelegramConnectionCode(null);
        contact.setTelegramCodeExpiresAt(null);
        EmergencyContact saved = emergencyContactRepository.save(contact);

        return new TelegramConnectResponse(
                saved.getId(),
                saved.getName(),
                null,
                telegramNotificationService.getBotUsername(),
                null,
                null,
                true,
                "Telegram successfully connected to emergency contact."
        );
    }

    /**
     * Disconnects Telegram alerts from an emergency contact.
     */
    @Transactional
    public TelegramConnectResponse disconnectTelegram(Long citizenId, Long contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));

        if (!contact.getUser().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this emergency contact");
        }

        telegramNotificationService.unlinkContact(contact);

        return new TelegramConnectResponse(
                contact.getId(),
                contact.getName(),
                null,
                telegramNotificationService.getBotUsername(),
                null,
                null,
                false,
                "Telegram alerts disconnected for this contact."
        );
    }

    private String generateSecureCode() {
        StringBuilder sb = new StringBuilder("SH-");
        for (int i = 0; i < 6; i++) {
            int idx = secureRandom.nextInt(CODE_ALPHABET.length());
            sb.append(CODE_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}
