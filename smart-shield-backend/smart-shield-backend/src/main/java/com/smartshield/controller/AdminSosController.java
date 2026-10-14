package com.smartshield.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshield.dto.SosAlertResponse;
import com.smartshield.dto.SosStatusHistoryResponse;
import com.smartshield.dto.UpdateSosStatusRequest;
import com.smartshield.model.AdminUser;
import com.smartshield.service.SosService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for Administrative SOS monitoring.
 * Automatically protected by AdminAuthInterceptor under /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/safety/sos")
public class AdminSosController {

    private final SosService sosService;

    public AdminSosController(SosService sosService) {
        this.sosService = sosService;
    }

    private String resolveAdminId(HttpServletRequest request) {
        if (request == null) {
            return "ADMIN";
        }
        String adminId = (String) request.getAttribute("adminId");
        if (adminId != null && !adminId.isBlank()) {
            return adminId;
        }
        Object adminObj = request.getAttribute("authenticatedAdmin");
        if (adminObj instanceof AdminUser adminUser && adminUser.getAdminId() != null) {
            return adminUser.getAdminId();
        }
        return "ADMIN";
    }

    /**
     * 1. GET /api/admin/safety/sos
     * Lists all emergency SOS alerts, newest first.
     */
    @GetMapping
    public ResponseEntity<List<SosAlertResponse>> getAllSosAlerts() {
        List<SosAlertResponse> alerts = sosService.getAllSosAlertsForAdmin();
        return ResponseEntity.ok(alerts);
    }

    /**
     * 2. GET /api/admin/safety/sos/{id}
     * Retrieves specific SOS alert details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SosAlertResponse> getSosAlertById(@PathVariable Long id) {
        SosAlertResponse alert = sosService.getSosAlertByIdForAdmin(id);
        return ResponseEntity.ok(alert);
    }

    /**
     * 3. GET /api/admin/safety/sos/{id}/history
     * Retrieves chronological status audit history for an SOS alert.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<SosStatusHistoryResponse>> getSosStatusHistory(@PathVariable Long id) {
        List<SosStatusHistoryResponse> history = sosService.getSosStatusHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * 4. PUT /api/admin/safety/sos/{id}/status
     * Updates SOS alert lifecycle status with administrative audit records.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<SosAlertResponse> updateSosStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSosStatusRequest request,
            HttpServletRequest httpRequest) {

        String adminId = resolveAdminId(httpRequest);
        SosAlertResponse response = sosService.updateSosStatusByAdmin(
                id,
                request.getStatus(),
                adminId,
                request.getRemarks()
        );
        return ResponseEntity.ok(response);
    }
}
