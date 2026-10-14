package com.smartshield.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.CreateSosRequest;
import com.smartshield.dto.SosAlertResponse;
import com.smartshield.model.User;
import com.smartshield.service.SosService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for citizen SOS emergency alert operations.
 * Automatically guarded by CitizenAuthInterceptor under /api/safety/**.
 */
@RestController
@RequestMapping("/api/safety/sos")
public class SosController {

    private final SosService sosService;

    public SosController(SosService sosService) {
        this.sosService = sosService;
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

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 1. POST /api/safety/sos
     * Triggers an emergency SOS alert with live browser geolocation.
     */
    @PostMapping
    public ResponseEntity<SosAlertResponse> createSos(
            @Valid @RequestBody CreateSosRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        String clientIp = resolveClientIp(httpRequest);
        SosAlertResponse response = sosService.createSos(citizenId, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /api/safety/sos/my
     * Retrieves all past SOS alerts triggered by the authenticated citizen.
     */
    @GetMapping("/my")
    public ResponseEntity<List<SosAlertResponse>> getMySosAlerts(HttpServletRequest httpRequest) {
        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        List<SosAlertResponse> alerts = sosService.getCitizenSosAlerts(citizenId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * 3. GET /api/safety/sos/{id}
     * Retrieves a specific SOS alert owned by the authenticated citizen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SosAlertResponse> getSosAlertById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        SosAlertResponse response = sosService.getCitizenSosAlertById(id, citizenId);
        return ResponseEntity.ok(response);
    }
}
