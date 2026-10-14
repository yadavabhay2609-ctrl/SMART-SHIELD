package com.smartshield.controller;

import java.util.List;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.ApiResponse;
import com.smartshield.dto.EmergencyContactRequest;
import com.smartshield.dto.EmergencyContactResponse;
import com.smartshield.dto.TelegramConnectResponse;
import com.smartshield.model.User;
import com.smartshield.service.EmergencyContactService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for citizen emergency contacts.
 * Automatically guarded by CitizenAuthInterceptor under /api/safety/**.
 */
@RestController
@RequestMapping("/api/safety/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    private Long resolveCitizenId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute("authenticatedCitizenId");
        if (attr instanceof Long l) {
            return l;
        }
        if (attr instanceof User u) {
            return u.getId();
        }
        return null;
    }

    /**
     * 1. GET /api/safety/emergency-contacts
     * Retrieves all emergency contacts for the authenticated citizen.
     */
    @GetMapping
    public ResponseEntity<List<EmergencyContactResponse>> getEmergencyContacts(HttpServletRequest httpRequest) {
        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        List<EmergencyContactResponse> contacts = emergencyContactService.getContactsForCitizen(citizenId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * 2. POST /api/safety/emergency-contacts
     * Adds a new emergency contact for the authenticated citizen.
     */
    @PostMapping
    public ResponseEntity<EmergencyContactResponse> createEmergencyContact(
            @Valid @RequestBody EmergencyContactRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        EmergencyContactResponse response = emergencyContactService.createContact(citizenId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3. PUT /api/safety/emergency-contacts/{id}
     * Updates an emergency contact belonging to the authenticated citizen.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmergencyContactResponse> updateEmergencyContact(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyContactRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        EmergencyContactResponse response = emergencyContactService.updateContact(citizenId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. DELETE /api/safety/emergency-contacts/{id}
     * Removes an emergency contact belonging to the authenticated citizen.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmergencyContact(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        emergencyContactService.deleteContact(citizenId, id);
        return ResponseEntity.ok(ApiResponse.success("Emergency contact removed successfully"));
    }

    /**
     * 5. POST /api/safety/emergency-contacts/{id}/telegram/connect
     * Generates a short-lived random single-use connection code and bot link.
     */
    @PostMapping("/{id}/telegram/connect")
    public ResponseEntity<TelegramConnectResponse> connectTelegram(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        TelegramConnectResponse response = emergencyContactService.generateTelegramConnectCode(citizenId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. GET /api/safety/emergency-contacts/{id}/telegram/status
     * Retrieves connection status and checks Telegram bot updates for pending codes.
     */
    @GetMapping("/{id}/telegram/status")
    public ResponseEntity<TelegramConnectResponse> getTelegramStatus(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        TelegramConnectResponse response = emergencyContactService.getTelegramStatus(citizenId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 7. POST /api/safety/emergency-contacts/{id}/telegram/verify
     * Manually verifies and links Telegram with connection code (useful for testing or direct linking).
     */
    @PostMapping("/{id}/telegram/verify")
    public ResponseEntity<TelegramConnectResponse> verifyTelegram(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        String code = body != null ? body.get("code") : null;
        String chatId = body != null ? body.get("chatId") : null;

        TelegramConnectResponse response = emergencyContactService.verifyAndLinkTelegram(citizenId, id, chatId, code);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. POST /api/safety/emergency-contacts/{id}/telegram/disconnect
     * Disconnects Telegram alerts from this emergency contact.
     */
    @PostMapping("/{id}/telegram/disconnect")
    public ResponseEntity<TelegramConnectResponse> disconnectTelegram(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        TelegramConnectResponse response = emergencyContactService.disconnectTelegram(citizenId, id);
        return ResponseEntity.ok(response);
    }
}
