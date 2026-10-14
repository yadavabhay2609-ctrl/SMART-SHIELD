package com.smartshield.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartshield.controller.AdminSettingsController;
import com.smartshield.dto.CreateGrievanceRequest;
import com.smartshield.dto.GrievanceResponse;
import com.smartshield.model.GrievanceCategory;
import com.smartshield.model.GrievancePriority;
import com.smartshield.model.SafeRoute;
import com.smartshield.repository.SafeRouteRepository;
import com.smartshield.repository.UserRepository;

/**
 * Service managing automated grievance creation when routes meet configured safety risk thresholds.
 * Respects system settings (auto_file_enabled, auto_file_threshold, min_ratings_for_auto_file)
 * and strictly prevents duplicate filings.
 */
@Service
public class AutoGrievanceService {

    private static final Logger log = LoggerFactory.getLogger(AutoGrievanceService.class);

    private final SystemSettingService systemSettingService;
    private final GrievanceService grievanceService;
    private final SafeRouteRepository safeRouteRepository;
    private final UserRepository userRepository;

    public AutoGrievanceService(SystemSettingService systemSettingService,
                                GrievanceService grievanceService,
                                SafeRouteRepository safeRouteRepository,
                                UserRepository userRepository) {
        this.systemSettingService = systemSettingService;
        this.grievanceService = grievanceService;
        this.safeRouteRepository = safeRouteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Checks if route meets auto-file conditions and generates a public safety grievance through the
     * existing GrievanceService if enabled and not already filed.
     *
     * @param route the safe route entity to evaluate
     * @param triggeringCitizenId citizen user ID associated with the latest rating or route calculation
     */
    @Transactional
    public boolean checkAndAutoFileGrievance(SafeRoute route, Long triggeringCitizenId) {
        if (route == null) {
            return false;
        }

        // 1. Check duplicate prevention
        if (Boolean.TRUE.equals(route.getAutoGrievanceFiled())) {
            log.debug("Route {} already has auto-filed grievance: {}", route.getSafeRouteId(), route.getAutoGrievanceTrackingId());
            return false;
        }

        // 2. Read dynamic system settings
        boolean enabled = Boolean.parseBoolean(
                systemSettingService.getSettingValue(AdminSettingsController.KEY_AUTO_FILE_ENABLED, "false"));
        if (!enabled) {
            return false;
        }

        int threshold = Integer.parseInt(
                systemSettingService.getSettingValue(AdminSettingsController.KEY_AUTO_FILE_THRESHOLD, "3"));
        int minRatings = Integer.parseInt(
                systemSettingService.getSettingValue(AdminSettingsController.KEY_MIN_RATINGS_FOR_AUTO_FILE, "5"));

        // Never auto-file when route has INSUFFICIENT_DATA
        if ("INSUFFICIENT_DATA".equalsIgnoreCase(route.getSafetyLevel())) {
            log.debug("Route {} has INSUFFICIENT_DATA safety level. Skipping auto-grievance.", route.getSafeRouteId());
            return false;
        }

        // 3. Condition: Either rating threshold met with sufficient ratings, OR extreme low safety score with verified evidence
        boolean ratingConditionMet = (route.getRatingCount() != null && route.getRatingCount() >= minRatings)
                && (route.getAverageRating() != null && route.getAverageRating() <= threshold);

        boolean scoreConditionMet = (route.getSafetyScore() != null && route.getSafetyScore() > 0 && route.getSafetyScore() <= 25);

        if (!ratingConditionMet && !scoreConditionMet) {
            return false;
        }

        // 4. Resolve citizen ID to file grievance under
        Long filingCitizenId = triggeringCitizenId;
        if (filingCitizenId == null) {
            filingCitizenId = userRepository.findAll().stream()
                    .findFirst()
                    .map(u -> u.getId())
                    .orElse(null);
        }

        if (filingCitizenId == null) {
            log.warn("Cannot auto-file grievance for route {}: no citizen user found in system.", route.getSafeRouteId());
            return false;
        }

        try {
            String location = (route.getOriginAddress() != null && !route.getOriginAddress().isBlank())
                    ? route.getOriginAddress()
                    : String.format("%.4f, %.4f", route.getOriginLatitude(), route.getOriginLongitude());

            String title = "[Auto-Filed] Public Safety Hazard: High-Risk Route " + route.getSafeRouteId();
            String description = String.format(
                    "Automated Smart Shield alert: Route %s exhibits heightened risk indicators. " +
                    "Safety Score: %d/100 (%s). Average Community Rating: %.1f/5 stars (%d reviews). " +
                    "Corridor: Origin (%.4f, %.4f) to Destination (%.4f, %.4f). Inspection and enhanced patrol requested.",
                    route.getSafeRouteId(),
                    route.getSafetyScore(),
                    route.getSafetyLevel(),
                    route.getAverageRating() != null ? route.getAverageRating() : 0.0,
                    route.getRatingCount() != null ? route.getRatingCount() : 0,
                    route.getOriginLatitude(), route.getOriginLongitude(),
                    route.getDestinationLatitude(), route.getDestinationLongitude()
            );

            CreateGrievanceRequest request = new CreateGrievanceRequest(
                    GrievanceCategory.PUBLIC_SAFETY,
                    title,
                    description,
                    location,
                    GrievancePriority.HIGH
            );

            GrievanceResponse response = grievanceService.createGrievance(filingCitizenId, request, "SAFE_ROUTE_AUTO");

            route.setAutoGrievanceFiled(true);
            route.setAutoGrievanceTrackingId(response.getTrackingId());
            safeRouteRepository.save(route);

            log.info("Successfully auto-filed public safety grievance {} for route {}", response.getTrackingId(), route.getSafeRouteId());
            return true;
        } catch (Exception e) {
            log.error("Failed to auto-file grievance for route {}: {}", route.getSafeRouteId(), e.getMessage());
            return false;
        }
    }
}
