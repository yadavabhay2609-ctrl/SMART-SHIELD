package com.smartshield.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.CreateGrievanceRequest;
import com.smartshield.dto.GrievanceEvidenceResponse;
import com.smartshield.dto.GrievanceResponse;
import com.smartshield.dto.GrievanceStatusHistoryResponse;
import com.smartshield.model.Grievance;
import com.smartshield.model.GrievanceEvidence;
import com.smartshield.model.User;
import com.smartshield.repository.GrievanceEvidenceRepository;
import com.smartshield.repository.GrievanceRepository;
import com.smartshield.service.GrievanceEvidenceStorageService;
import com.smartshield.service.GrievanceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for citizen grievance operations.
 * Enforces ownership validation, zero-trust header protection,
 * and safe evidence streaming without leaking server filesystem internals.
 */
@RestController
@RequestMapping("/api/grievances")
public class GrievanceController {

    private final GrievanceService grievanceService;
    private final GrievanceRepository grievanceRepository;
    private final GrievanceEvidenceRepository grievanceEvidenceRepository;
    private final GrievanceEvidenceStorageService grievanceEvidenceStorageService;

    public GrievanceController(GrievanceService grievanceService,
                               GrievanceRepository grievanceRepository,
                               GrievanceEvidenceRepository grievanceEvidenceRepository,
                               GrievanceEvidenceStorageService grievanceEvidenceStorageService) {
        this.grievanceService = grievanceService;
        this.grievanceRepository = grievanceRepository;
        this.grievanceEvidenceRepository = grievanceEvidenceRepository;
        this.grievanceEvidenceStorageService = grievanceEvidenceStorageService;
    }

    /**
     * Resolves the trusted authenticated citizen from server-managed request attributes.
     * In the current architecture, citizen authentication does not yet issue session/JWT tokens.
     * To prevent spoofing and IDOR vulnerabilities, this method strictly returns null when
     * no trusted server-validated principal exists, and NEVER trusts client-supplied headers
     * (e.g. X-Citizen-Id).
     */
    private Long resolveAuthenticatedCitizenId(HttpServletRequest request) {
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
     * 1. CREATE GRIEVANCE
     * POST /api/grievances
     */
    @PostMapping
    public ResponseEntity<GrievanceResponse> createGrievance(
            @Valid @RequestBody CreateGrievanceRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required. Please sign in with a valid citizen account."
            );
        }

        GrievanceResponse response = grievanceService.createGrievance(citizenId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET GRIEVANCE BY TRACKING ID (Citizen-specific with ownership check)
     * GET /api/grievances/{trackingId}
     */
    @GetMapping("/{trackingId}")
    public ResponseEntity<GrievanceResponse> getGrievanceByTrackingId(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required to access this grievance. Ownership must be verified."
            );
        }

        GrievanceResponse response = grievanceService.getGrievanceByTrackingIdForCitizen(trackingId, citizenId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. GET CITIZEN'S GRIEVANCES
     * GET /api/grievances/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<GrievanceResponse>> getMyGrievances(HttpServletRequest httpRequest) {
        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required. Please sign in with a valid citizen account."
            );
        }

        List<GrievanceResponse> responses = grievanceService.getGrievancesByCitizen(citizenId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 4. GET STATUS HISTORY
     * GET /api/grievances/{trackingId}/history
     */
    @GetMapping("/{trackingId}/history")
    public ResponseEntity<List<GrievanceStatusHistoryResponse>> getStatusHistory(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required to view audit history. Ownership must be verified."
            );
        }

        // Verify grievance ownership first
        grievanceService.getGrievanceByTrackingIdForCitizen(trackingId, citizenId);

        List<GrievanceStatusHistoryResponse> history = grievanceService.getGrievanceStatusHistory(trackingId);
        return ResponseEntity.ok(history);
    }

    /**
     * 5. EVIDENCE UPLOAD
     * POST /api/grievances/{trackingId}/evidence
     */
    @PostMapping(value = "/{trackingId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GrievanceEvidenceResponse> uploadEvidence(
            @PathVariable String trackingId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required to upload evidence. Ownership must be verified."
            );
        }

        // Verify grievance exists
        Grievance grievance = grievanceRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        // Enforce ownership: only the citizen who filed the grievance may attach evidence
        if (grievance.getCitizen() == null || !citizenId.equals(grievance.getCitizen().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: Grievance does not belong to the authenticated citizen"
            );
        }

        // Validate and store evidence file safely using GrievanceEvidenceStorageService
        GrievanceEvidenceStorageService.StoredEvidence stored;
        try {
            stored = grievanceEvidenceStorageService.storeEvidence(file);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File path traversal rejected: " + e.getMessage());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded evidence file", e);
        }

        // Persist GrievanceEvidence record linking to the grievance
        GrievanceEvidence evidence = new GrievanceEvidence(
                grievance,
                stored.storedFileName(),
                stored.originalFileName(),
                stored.filePath(),
                stored.contentType(),
                stored.fileSize()
        );
        GrievanceEvidence savedEvidence = grievanceEvidenceRepository.save(evidence);

        return ResponseEntity.status(HttpStatus.CREATED).body(GrievanceEvidenceResponse.fromEntity(savedEvidence));
    }

    /**
     * 6. SAFE EVIDENCE READ ENDPOINT
     * GET /api/grievances/{trackingId}/evidence
     */
    @GetMapping("/{trackingId}/evidence")
    public ResponseEntity<List<GrievanceEvidenceResponse>> getEvidenceMetadata(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveAuthenticatedCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Citizen authentication required to view evidence metadata. Ownership must be verified."
            );
        }

        // Verify grievance ownership
        Grievance grievance = grievanceRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        if (grievance.getCitizen() == null || !citizenId.equals(grievance.getCitizen().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: Grievance does not belong to the authenticated citizen"
            );
        }

        List<GrievanceEvidenceResponse> evidenceResponses = grievanceEvidenceRepository
                .findByGrievanceIdOrderByUploadedAtAsc(grievance.getId())
                .stream()
                .map(GrievanceEvidenceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(evidenceResponses);
    }
}
