package com.smartshield.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.CreateSosRequest;
import com.smartshield.dto.SosAlertResponse;
import com.smartshield.dto.SosStatusHistoryResponse;
import com.smartshield.model.EmergencyContact;
import com.smartshield.model.SosAlert;
import com.smartshield.model.SosNotificationStatus;
import com.smartshield.model.SosStatus;
import com.smartshield.model.SosStatusHistory;
import com.smartshield.model.User;
import com.smartshield.repository.EmergencyContactRepository;
import com.smartshield.repository.SosAlertRepository;
import com.smartshield.repository.SosStatusHistoryRepository;
import com.smartshield.repository.UserRepository;
import com.smartshield.util.RateLimiterUtil;

@Service
public class SosService {

    private final SosAlertRepository sosAlertRepository;
    private final SosStatusHistoryRepository sosStatusHistoryRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;
    private final SosNotificationService sosNotificationService;
    private final TelegramNotificationService telegramNotificationService;
    private final RateLimiterUtil rateLimiterUtil;

    // Rate limits: 3 SOS alerts per citizen per 60 seconds
    private static final int MAX_SOS_PER_WINDOW = 3;
    private static final long SOS_WINDOW_MILLIS = 60_000L;

    public SosService(SosAlertRepository sosAlertRepository,
                      SosStatusHistoryRepository sosStatusHistoryRepository,
                      EmergencyContactRepository emergencyContactRepository,
                      UserRepository userRepository,
                      SosNotificationService sosNotificationService,
                      TelegramNotificationService telegramNotificationService,
                      RateLimiterUtil rateLimiterUtil) {
        this.sosAlertRepository = sosAlertRepository;
        this.sosStatusHistoryRepository = sosStatusHistoryRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.userRepository = userRepository;
        this.sosNotificationService = sosNotificationService;
        this.telegramNotificationService = telegramNotificationService;
        this.rateLimiterUtil = rateLimiterUtil;
    }

    /**
     * Creates a persistent SOS alert, verifies rate limits, loads active emergency contacts,
     * dispatches notifications if enabled, and logs status audit history.
     */
    @Transactional
    public SosAlertResponse createSos(Long citizenId, CreateSosRequest request, String clientIp) {
        // 1. Abuse Protection & Rate Limiting per citizen
        String citizenKey = "SOS_CITIZEN_" + citizenId;
        if (!rateLimiterUtil.tryAcquire(citizenKey, MAX_SOS_PER_WINDOW, SOS_WINDOW_MILLIS)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many SOS requests. Please wait a moment before sending another alert."
            );
        }

        // Secondary IP rate limiting to prevent flooding
        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = "SOS_IP_" + clientIp;
            if (!rateLimiterUtil.tryAcquire(ipKey, 10, SOS_WINDOW_MILLIS)) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Request rate limit exceeded. Please wait a moment."
                );
            }
        }

        // 2. Fetch authenticated citizen
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen user not found"));

        // 3. Persist initial SOS Alert record
        SosAlert alert = new SosAlert(
                citizen,
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracy(),
                request.getSource(),
                request.getMessage()
        );
        alert.setStatus(SosStatus.TRIGGERED);
        alert.setNotificationStatus(SosNotificationStatus.PENDING);
        alert.setTriggeredAt(LocalDateTime.now());

        SosAlert savedAlert = sosAlertRepository.save(alert);

        // 4. Record initial audit status history
        SosStatusHistory initialHistory = new SosStatusHistory(
                savedAlert,
                null,
                SosStatus.TRIGGERED,
                citizen.getEmail(),
                "SOS alert triggered by citizen"
        );
        sosStatusHistoryRepository.save(initialHistory);

        // 5. Retrieve active emergency contacts for this citizen
        List<EmergencyContact> activeContacts = emergencyContactRepository.findByUserIdAndActiveTrue(citizenId);

        // 6. Attempt Email notification dispatch
        SosNotificationService.NotificationResult emailResult = sosNotificationService.notifyContacts(savedAlert, activeContacts);
        savedAlert.setEmailStatus(emailResult.getStatus().name());

        // 7. Attempt Telegram notification dispatch
        TelegramNotificationService.TelegramResult telegramResult = telegramNotificationService.sendSosNotification(savedAlert, activeContacts);
        savedAlert.setTelegramStatus(telegramResult.getStatus());

        // 8. Aggregate honest overall notification status
        boolean anySent = (emailResult.getStatus() == SosNotificationStatus.SENT) || "SENT".equals(telegramResult.getStatus());
        boolean anyFailed = (emailResult.getStatus() == SosNotificationStatus.FAILED) || "FAILED".equals(telegramResult.getStatus());

        if (anySent) {
            savedAlert.setNotificationStatus(SosNotificationStatus.SENT);
            savedAlert.setStatus(SosStatus.NOTIFIED);
        } else if (anyFailed) {
            savedAlert.setNotificationStatus(SosNotificationStatus.FAILED);
        } else {
            savedAlert.setNotificationStatus(SosNotificationStatus.NOT_CONFIGURED);
        }

        String combinedDetails = "Email: " + emailResult.getDetails() + " | Telegram: " + telegramResult.getDetails();
        savedAlert.setNotificationDetails(combinedDetails);

        if (anySent) {
            SosStatusHistory notifiedHistory = new SosStatusHistory(
                    savedAlert,
                    SosStatus.TRIGGERED,
                    SosStatus.NOTIFIED,
                    "SYSTEM",
                    "Emergency notifications dispatched: Email=" + emailResult.getStatus() + ", Telegram=" + telegramResult.getStatus()
            );
            sosStatusHistoryRepository.save(notifiedHistory);
        }

        SosAlert finalAlert = sosAlertRepository.save(savedAlert);
        return SosAlertResponse.fromEntity(finalAlert, activeContacts.size());
    }

    /**
     * Lists all SOS alerts for the authenticated citizen, ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<SosAlertResponse> getCitizenSosAlerts(Long citizenId) {
        return sosAlertRepository.findByCitizenIdOrderByTriggeredAtDesc(citizenId)
                .stream()
                .map(alert -> {
                    int count = emergencyContactRepository.findByUserIdAndActiveTrue(citizenId).size();
                    return SosAlertResponse.fromEntity(alert, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves specific SOS alert for a citizen with strict ownership check.
     */
    @Transactional(readOnly = true)
    public SosAlertResponse getCitizenSosAlertById(Long alertId, Long citizenId) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found"));

        if (!alert.getCitizen().getId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this SOS alert");
        }

        int count = emergencyContactRepository.findByUserIdAndActiveTrue(citizenId).size();
        return SosAlertResponse.fromEntity(alert, count);
    }

    /**
     * Lists all SOS alerts across all citizens for Admin monitoring.
     */
    @Transactional(readOnly = true)
    public List<SosAlertResponse> getAllSosAlertsForAdmin() {
        return sosAlertRepository.findAllByOrderByTriggeredAtDesc()
                .stream()
                .map(alert -> SosAlertResponse.fromEntity(alert, null))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single SOS alert for Admin monitoring.
     */
    @Transactional(readOnly = true)
    public SosAlertResponse getSosAlertByIdForAdmin(Long alertId) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found with ID: " + alertId));
        return SosAlertResponse.fromEntity(alert, null);
    }

    /**
     * Retrieves status audit history for an SOS alert.
     */
    @Transactional(readOnly = true)
    public List<SosStatusHistoryResponse> getSosStatusHistory(Long alertId) {
        if (!sosAlertRepository.existsById(alertId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found with ID: " + alertId);
        }

        return sosStatusHistoryRepository.findBySosAlertIdOrderByChangedAtAsc(alertId)
                .stream()
                .map(SosStatusHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Updates SOS alert lifecycle status by an authorized administrator.
     */
    @Transactional
    public SosAlertResponse updateSosStatusByAdmin(Long alertId, SosStatus newStatus, String adminId, String remarks) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found with ID: " + alertId));

        SosStatus oldStatus = alert.getStatus();
        alert.setStatus(newStatus);
        SosAlert updatedAlert = sosAlertRepository.save(alert);

        String remarkText = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Status updated by admin " + adminId;
        SosStatusHistory history = new SosStatusHistory(
                updatedAlert,
                oldStatus,
                newStatus,
                adminId,
                remarkText
        );
        sosStatusHistoryRepository.save(history);

        return SosAlertResponse.fromEntity(updatedAlert, null);
    }
}
