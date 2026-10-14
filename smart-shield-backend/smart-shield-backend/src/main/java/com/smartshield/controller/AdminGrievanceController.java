package com.smartshield.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.GrievanceEvidenceResponse;
import com.smartshield.dto.GrievanceResponse;
import com.smartshield.dto.GrievanceStatusHistoryResponse;
import com.smartshield.dto.UpdateGrievanceStatusRequest;
import com.smartshield.model.AdminUser;
import com.smartshield.model.Grievance;
import com.smartshield.repository.GrievanceEvidenceRepository;
import com.smartshield.repository.GrievanceRepository;
import com.smartshield.service.GrievanceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller providing administrative grievance management APIs.
 * Automatically protected by AdminAuthInterceptor under /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/grievances")
public class AdminGrievanceController {

    private final GrievanceService grievanceService;
    private final GrievanceRepository grievanceRepository;
    private final GrievanceEvidenceRepository grievanceEvidenceRepository;

    public AdminGrievanceController(GrievanceService grievanceService,
                                    GrievanceRepository grievanceRepository,
                                    GrievanceEvidenceRepository grievanceEvidenceRepository) {
        this.grievanceService = grievanceService;
        this.grievanceRepository = grievanceRepository;
        this.grievanceEvidenceRepository = grievanceEvidenceRepository;
    }

    /**
     * 1. GET /api/admin/grievances
     * Lists all grievances, newest first.
     */
    @GetMapping
    public ResponseEntity<List<GrievanceResponse>> getAllGrievances() {
        List<GrievanceResponse> responses = grievanceService.getAllGrievances();
        return ResponseEntity.ok(responses);
    }

    /**
     * 2. GET /api/admin/grievances/{trackingId}
     * Retrieves specific grievance details by tracking ID.
     */
    @GetMapping("/{trackingId}")
    public ResponseEntity<GrievanceResponse> getGrievanceByTrackingId(@PathVariable String trackingId) {
        GrievanceResponse response = grievanceService.getGrievanceByTrackingId(trackingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. GET /api/admin/grievances/{trackingId}/history
     * Retrieves the chronological status audit history for a grievance.
     */
    @GetMapping("/{trackingId}/history")
    public ResponseEntity<List<GrievanceStatusHistoryResponse>> getGrievanceHistory(@PathVariable String trackingId) {
        List<GrievanceStatusHistoryResponse> history = grievanceService.getGrievanceStatusHistory(trackingId);
        return ResponseEntity.ok(history);
    }

    /**
     * 4. PUT /api/admin/grievances/{trackingId}/status
     * Updates grievance status, records audit history with server-managed admin identity.
     */
    @PutMapping("/{trackingId}/status")
    public ResponseEntity<GrievanceResponse> updateGrievanceStatus(
            @PathVariable String trackingId,
            @Valid @RequestBody UpdateGrievanceStatusRequest request,
            HttpServletRequest httpRequest) {

        // Derive trusted admin identity from server-managed request attributes set by AdminAuthInterceptor
        String adminId = (String) httpRequest.getAttribute("adminId");
        if (adminId == null || adminId.isBlank()) {
            AdminUser admin = (AdminUser) httpRequest.getAttribute("authenticatedAdmin");
            adminId = (admin != null && admin.getAdminId() != null) ? admin.getAdminId() : "ADMIN";
        }

        GrievanceResponse response = grievanceService.updateGrievanceStatus(
                trackingId,
                request.getStatus(),
                adminId,
                request.getRemarks()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 5. GET /api/admin/grievances/{trackingId}/evidence
     * Retrieves safe evidence metadata without leaking internal file paths.
     */
    @GetMapping("/{trackingId}/evidence")
    public ResponseEntity<List<GrievanceEvidenceResponse>> getEvidenceMetadata(@PathVariable String trackingId) {
        Grievance grievance = grievanceRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        List<GrievanceEvidenceResponse> evidenceList = grievanceEvidenceRepository
                .findByGrievanceIdOrderByUploadedAtAsc(grievance.getId())
                .stream()
                .map(GrievanceEvidenceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(evidenceList);
    }
}
