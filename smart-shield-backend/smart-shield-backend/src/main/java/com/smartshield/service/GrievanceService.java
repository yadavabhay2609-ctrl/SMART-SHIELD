package com.smartshield.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.CreateGrievanceRequest;
import com.smartshield.dto.GrievanceResponse;
import com.smartshield.dto.GrievanceStatusHistoryResponse;
import com.smartshield.model.Grievance;
import com.smartshield.model.GrievancePriority;
import com.smartshield.model.GrievanceStatus;
import com.smartshield.model.GrievanceStatusHistory;
import com.smartshield.model.User;
import com.smartshield.repository.GrievanceRepository;
import com.smartshield.repository.GrievanceStatusHistoryRepository;
import com.smartshield.repository.UserRepository;
import com.smartshield.util.TrackingIdGenerator;

/**
 * Core business service for managing citizen grievances, deterministic tracking IDs,
 * citizen ownership enforcement, and status audit histories.
 */
@Service
public class GrievanceService {

    private static final Logger log = LoggerFactory.getLogger(GrievanceService.class);

    private final GrievanceRepository grievanceRepository;
    private final GrievanceStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final TrackingIdGenerator trackingIdGenerator;
    private final SosNotificationService sosNotificationService;
    private final TelegramNotificationService telegramNotificationService;

    @Autowired
    public GrievanceService(GrievanceRepository grievanceRepository,
                            GrievanceStatusHistoryRepository statusHistoryRepository,
                            UserRepository userRepository,
                            TrackingIdGenerator trackingIdGenerator,
                            @Autowired(required = false) SosNotificationService sosNotificationService,
                            @Autowired(required = false) TelegramNotificationService telegramNotificationService) {
        this.grievanceRepository = grievanceRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
        this.trackingIdGenerator = trackingIdGenerator;
        this.sosNotificationService = sosNotificationService;
        this.telegramNotificationService = telegramNotificationService;
    }

    public GrievanceService(GrievanceRepository grievanceRepository,
                            GrievanceStatusHistoryRepository statusHistoryRepository,
                            UserRepository userRepository,
                            TrackingIdGenerator trackingIdGenerator) {
        this(grievanceRepository, statusHistoryRepository, userRepository, trackingIdGenerator, null, null);
    }

    /**
     * Creates a new grievance for a validated citizen, generates a sequential tracking ID
     * from the persisted database primary key, and records the initial PENDING status history.
     *
     * @param citizenId database ID of the filing citizen
     * @param request grievance payload details
     * @return GrievanceResponse containing complete grievance details with tracking ID
     */
    @Transactional
    public GrievanceResponse createGrievance(Long citizenId, CreateGrievanceRequest request) {
        return createGrievance(citizenId, request, "CITIZEN");
    }

    @Transactional
    public GrievanceResponse createGrievance(Long citizenId, CreateGrievanceRequest request, String source) {
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Citizen ID is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grievance request cannot be null");
        }

        // 1. Validate that the referenced citizen exists
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Citizen not found with ID: " + citizenId
                ));

        // 2. Determine priority with fallback to MEDIUM
        GrievancePriority priority = (request.getPriority() != null)
                ? request.getPriority()
                : GrievancePriority.MEDIUM;

        String safeSource = (source != null && !source.isBlank()) ? source : "CITIZEN";

        // 3. Instantiate grievance with a temporary unique placeholder to satisfy DB non-null & uniqueness
        String tempTrackingId = "SS-TEMP-" + UUID.randomUUID();
        Grievance grievance = new Grievance(
                tempTrackingId,
                citizen,
                request.getCategory(),
                priority,
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getLocation().trim(),
                safeSource
        );

        // 4. Persist the grievance first so database generates auto-increment ID
        Grievance savedGrievance = grievanceRepository.save(grievance);

        // 5. Generate and assign the official deterministic tracking ID (e.g. SS-2026-0001)
        String officialTrackingId = trackingIdGenerator.generate(savedGrievance.getId());
        savedGrievance.setTrackingId(officialTrackingId);

        // 6. Persist the final grievance with official tracking ID
        savedGrievance = grievanceRepository.save(savedGrievance);

        // 7. Record initial status history: oldStatus=null, newStatus=PENDING
        GrievanceStatusHistory initialHistory = new GrievanceStatusHistory(
                savedGrievance,
                null,
                GrievanceStatus.PENDING,
                safeSource,
                safeSource.equals("CITIZEN") ? "Initial grievance submitted by citizen" : "Automatically filed based on safety risk threshold"
        );
        statusHistoryRepository.save(initialHistory);

        // 8. Safely dispatch email confirmation and telegram notifications (non-blocking)
        try {
            if (sosNotificationService != null) {
                sosNotificationService.sendGrievanceConfirmation(savedGrievance);
            }
            if (telegramNotificationService != null) {
                telegramNotificationService.sendGrievanceReceivedNotification(savedGrievance);
            }
        } catch (Exception e) {
            log.warn("Notification dispatch notice during grievance creation: {}", e.getMessage());
        }

        return GrievanceResponse.fromEntity(savedGrievance);
    }

    /**
     * Reads a grievance by its unique tracking ID.
     */
    @Transactional(readOnly = true)
    public GrievanceResponse getGrievanceByTrackingId(String trackingId) {
        if (trackingId == null || trackingId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking ID cannot be blank");
        }

        Grievance grievance = grievanceRepository.findByTrackingId(trackingId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        return GrievanceResponse.fromEntity(grievance);
    }

    /**
     * Reads a grievance by its tracking ID while enforcing citizen ownership.
     */
    @Transactional(readOnly = true)
    public GrievanceResponse getGrievanceByTrackingIdForCitizen(String trackingId, Long citizenId) {
        if (trackingId == null || trackingId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking ID cannot be blank");
        }
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Citizen ID is required");
        }

        Grievance grievance = grievanceRepository.findByTrackingId(trackingId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        // Enforce ownership: grievance citizen ID must match caller citizen ID
        if (grievance.getCitizen() == null || !citizenId.equals(grievance.getCitizen().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: Grievance does not belong to citizen"
            );
        }

        return GrievanceResponse.fromEntity(grievance);
    }

    /**
     * Reads a grievance by its database primary key.
     */
    @Transactional(readOnly = true)
    public GrievanceResponse getGrievanceById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grievance ID cannot be null");
        }

        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with ID: " + id
                ));

        return GrievanceResponse.fromEntity(grievance);
    }

    /**
     * Internal method to retrieve the Grievance entity instance (e.g. for evidence attachment).
     */
    @Transactional(readOnly = true)
    public Grievance getGrievanceEntity(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grievance ID cannot be null");
        }

        return grievanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with ID: " + id
                ));
    }

    /**
     * Retrieves the chronological audit history for a grievance by its tracking ID.
     */
    @Transactional(readOnly = true)
    public List<GrievanceStatusHistoryResponse> getGrievanceStatusHistory(String trackingId) {
        if (trackingId == null || trackingId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking ID cannot be blank");
        }

        Grievance grievance = grievanceRepository.findByTrackingId(trackingId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        return statusHistoryRepository.findByGrievanceIdOrderByChangedAtAsc(grievance.getId())
                .stream()
                .map(GrievanceStatusHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the chronological audit history for a grievance by its internal database ID.
     */
    @Transactional(readOnly = true)
    public List<GrievanceStatusHistoryResponse> getGrievanceStatusHistory(Long grievanceId) {
        if (grievanceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grievance ID cannot be null");
        }

        if (!grievanceRepository.existsById(grievanceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Grievance not found with ID: " + grievanceId
            );
        }

        return statusHistoryRepository.findByGrievanceIdOrderByChangedAtAsc(grievanceId)
                .stream()
                .map(GrievanceStatusHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all grievances submitted by a specific citizen, ordered by most recent first.
     */
    @Transactional(readOnly = true)
    public List<GrievanceResponse> getGrievancesByCitizen(Long citizenId) {
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Citizen ID is required");
        }

        if (!userRepository.existsById(citizenId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Citizen not found with ID: " + citizenId
            );
        }

        return grievanceRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId)
                .stream()
                .map(GrievanceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all grievances in the system, ordered by most recent first (for admin/monitoring use).
     */
    @Transactional(readOnly = true)
    public List<GrievanceResponse> getAllGrievances() {
        return grievanceRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(GrievanceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Controlled service method to update grievance status and log audit history.
     *
     * @param trackingId tracking ID of the grievance to update
     * @param newStatus new target status
     * @param changedBy name or role of modifier (e.g. "ADMIN", admin username)
     * @param remarks remarks or reason for status change
     * @return updated GrievanceResponse
     */
    @Transactional
    public GrievanceResponse updateGrievanceStatus(String trackingId,
                                                  GrievanceStatus newStatus,
                                                  String changedBy,
                                                  String remarks) {
        if (trackingId == null || trackingId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking ID cannot be blank");
        }
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New status is required");
        }

        Grievance grievance = grievanceRepository.findByTrackingId(trackingId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with tracking ID: " + trackingId
                ));

        GrievanceStatus oldStatus = grievance.getStatus();
        grievance.setStatus(newStatus);
        grievance.setUpdatedAt(LocalDateTime.now());
        Grievance updatedGrievance = grievanceRepository.save(grievance);

        String safeChangedBy = (changedBy != null && !changedBy.isBlank()) ? changedBy.trim() : "ADMIN";
        String safeRemarks = (remarks != null && !remarks.isBlank())
                ? remarks.trim()
                : ("Status updated to " + newStatus.name());

        GrievanceStatusHistory history = new GrievanceStatusHistory(
                updatedGrievance,
                oldStatus,
                newStatus,
                safeChangedBy,
                safeRemarks
        );
        statusHistoryRepository.save(history);

        // Safely dispatch status update notifications via email and telegram (non-blocking)
        try {
            if (sosNotificationService != null) {
                sosNotificationService.sendGrievanceStatusUpdate(updatedGrievance, oldStatus, newStatus, safeRemarks);
            }
            if (telegramNotificationService != null) {
                telegramNotificationService.sendGrievanceStatusUpdatedNotification(updatedGrievance, oldStatus, newStatus, safeRemarks);
            }
        } catch (Exception e) {
            log.warn("Notification dispatch notice during grievance status update: {}", e.getMessage());
        }

        return GrievanceResponse.fromEntity(updatedGrievance);
    }

    /**
     * Overloaded method to update grievance status by database ID.
     */
    @Transactional
    public GrievanceResponse updateGrievanceStatus(Long grievanceId,
                                                  GrievanceStatus newStatus,
                                                  String changedBy,
                                                  String remarks) {
        if (grievanceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grievance ID cannot be null");
        }

        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grievance not found with ID: " + grievanceId
                ));

        return updateGrievanceStatus(grievance.getTrackingId(), newStatus, changedBy, remarks);
    }
}
