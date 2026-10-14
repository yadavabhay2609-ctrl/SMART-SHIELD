package com.smartshield.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.AdminRouteSummaryResponse;
import com.smartshield.dto.NearbyFacilityResponse;
import com.smartshield.dto.RouteRatingRequest;
import com.smartshield.dto.RouteRatingResponse;
import com.smartshield.dto.SafeRouteConfigResponse;
import com.smartshield.dto.SafeRouteRequest;
import com.smartshield.dto.SafeRouteResponse;
import com.smartshield.model.SafeRoute;
import com.smartshield.model.SafeRouteRating;
import com.smartshield.model.User;
import com.smartshield.repository.SafeRouteRatingRepository;
import com.smartshield.repository.SafeRouteRepository;
import com.smartshield.repository.UserRepository;
import com.smartshield.util.RateLimiterUtil;

@Service
public class SafeRouteService {

    private static final Logger log = LoggerFactory.getLogger(SafeRouteService.class);

    private final SafeRouteRepository safeRouteRepository;
    private final SafeRouteRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final GoogleRoutesService googleRoutesService;
    private final GooglePlacesService googlePlacesService;
    private final RouteSafetyScoreService safetyScoreService;
    private final AutoGrievanceService autoGrievanceService;
    private final LocationService locationService;
    private final RateLimiterUtil rateLimiterUtil;

    @Value("${smartshield.google.maps.browser-api-key:}")
    private String mapsBrowserApiKey;

    private static final int MAX_ROUTE_CALCULATIONS_PER_MIN = 12;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    public SafeRouteService(SafeRouteRepository safeRouteRepository,
                            SafeRouteRatingRepository ratingRepository,
                            UserRepository userRepository,
                            GoogleRoutesService googleRoutesService,
                            GooglePlacesService googlePlacesService,
                            RouteSafetyScoreService safetyScoreService,
                            AutoGrievanceService autoGrievanceService,
                            LocationService locationService,
                            RateLimiterUtil rateLimiterUtil) {
        this.safeRouteRepository = safeRouteRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.googleRoutesService = googleRoutesService;
        this.googlePlacesService = googlePlacesService;
        this.safetyScoreService = safetyScoreService;
        this.autoGrievanceService = autoGrievanceService;
        this.locationService = locationService;
        this.rateLimiterUtil = rateLimiterUtil;
    }

    /**
     * Calculates an analyzed Safe Route with safety score, levels, factors, and nearby infrastructure.
     */
    @Transactional
    public SafeRouteResponse calculateSafeRoute(Long citizenId, SafeRouteRequest request, String clientIp) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route request payload cannot be null");
        }

        // 1. Sliding-window rate limiting to protect external APIs and server resources
        String rateKey = "ROUTE_CALC_" + (citizenId != null ? "USER_" + citizenId : "IP_" + clientIp);
        if (!rateLimiterUtil.tryAcquire(rateKey, MAX_ROUTE_CALCULATIONS_PER_MIN, RATE_LIMIT_WINDOW_MS)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many route calculations. Please wait a moment before requesting another route."
            );
        }

        Double oLat = request.getOriginLatitude();
        Double oLng = request.getOriginLongitude();
        Double dLat = request.getDestinationLatitude();
        Double dLng = request.getDestinationLongitude();
        String mode = (request.getTravelMode() != null && !request.getTravelMode().isBlank())
                ? request.getTravelMode().toUpperCase() : "WALK";

        // Check if origin and destination are identical
        if (Math.abs(oLat - dLat) < 0.0001 && Math.abs(oLng - dLng) < 0.0001) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origin and destination coordinates cannot be identical");
        }

        int distanceMeters;
        int durationSeconds;
        String encodedPolyline = null;
        String description = "Direct Corridor";
        List<String> warnings = new ArrayList<>();

        // 2. Query Google Routes API if configured
        Optional<GoogleRoutesService.GoogleRouteData> googleDataOpt =
                googleRoutesService.computeRoute(oLat, oLng, dLat, dLng, mode);

        if (googleDataOpt.isPresent()) {
            GoogleRoutesService.GoogleRouteData gData = googleDataOpt.get();
            distanceMeters = gData.getDistanceMeters();
            durationSeconds = gData.getDurationSeconds();
            encodedPolyline = gData.getEncodedPolyline();
            description = (gData.getDescription() != null && !gData.getDescription().isBlank())
                    ? gData.getDescription() : "Google Routes Navigation";
            warnings.addAll(gData.getWarnings());
        } else if (request.getDistanceMeters() != null && request.getDistanceMeters() > 0) {
            // Real road routing metrics provided by Free OpenStreetMap / OSRM
            distanceMeters = request.getDistanceMeters();
            durationSeconds = (request.getDurationSeconds() != null && request.getDurationSeconds() > 0)
                    ? request.getDurationSeconds() : (int) Math.round(distanceMeters / ("WALK".equalsIgnoreCase(mode) ? 1.3 : 8.3));
            encodedPolyline = request.getEncodedPolyline();
            description = "OpenStreetMap / OSRM Road Navigation (" + mode + ")";
            warnings.add("Real road-following transit geometry provided by OpenStreetMap & OSRM.");
        } else {
            // Honest notification of unconfigured status with accurate baseline geometry calculation
            double directDistance = calculateHaversineDistance(oLat, oLng, dLat, dLng);
            distanceMeters = (int) Math.round(directDistance * 1.25); // Account for urban road network winding factor
            double speedMps = "WALK".equalsIgnoreCase(mode) ? 1.3 : 8.3; // 1.3 m/s (~4.7 km/h) walk, 8.3 m/s (~30 km/h) urban drive
            durationSeconds = (int) Math.round(distanceMeters / speedMps);
            description = "Standard Urban Corridor (" + mode + ")";
            warnings.add("Notice: Server running with baseline route geometry. To enable live Google Routes, configure GOOGLE_ROUTES_API_KEY.");
        }

        // 3. Query Google Places API or fallback to verified municipal emergency infrastructure
        double midpointLat = (oLat + dLat) / 2.0;
        double midpointLng = (oLng + dLng) / 2.0;
        List<NearbyFacilityResponse> facilities =
                googlePlacesService.findNearbySafetyFacilities(midpointLat, midpointLng, 2500);

        if (facilities.isEmpty()) {
            facilities = locationService.findNearbySafetyFacilities(midpointLat, midpointLng, 4500);
        }

        // 4. Generate deterministic/unique Smart Shield Route ID
        String safeRouteId = "SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 5. Evaluate Smart Shield Safety Score
        RouteSafetyScoreService.SafetyEvaluation eval = safetyScoreService.evaluateRouteSafety(
                oLat, oLng, dLat, dLng,
                request.getOriginAddress(), request.getDestinationAddress(),
                mode, safeRouteId, facilities
        );
        warnings.addAll(eval.getWarnings());

        // 6. Persist SafeRoute entity
        SafeRoute route = new SafeRoute(
                safeRouteId,
                oLat, oLng, dLat, dLng,
                request.getOriginAddress(),
                request.getDestinationAddress(),
                mode,
                distanceMeters,
                durationSeconds,
                eval.getSafetyScore(),
                eval.getSafetyLevel(),
                eval.getDataConfidence(),
                encodedPolyline,
                description
        );
        route.setRouteSummary(description);

        SafeRoute savedRoute = safeRouteRepository.save(route);

        // 7. Check dynamic auto-file conditions
        autoGrievanceService.checkAndAutoFileGrievance(savedRoute, citizenId);

        // 8. Assemble response DTO
        SafeRouteResponse response = SafeRouteResponse.fromEntity(savedRoute);
        response.setFactors(eval.getFactors());
        response.setNearbyFacilities(facilities);
        response.setWarnings(warnings);

        return response;
    }

    /**
     * Retrieves an existing SafeRoute by its identifier.
     */
    @Transactional(readOnly = true)
    public SafeRouteResponse getSafeRoute(String safeRouteId) {
        if (safeRouteId == null || safeRouteId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Safe route ID is required");
        }

        SafeRoute route = safeRouteRepository.findBySafeRouteId(safeRouteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Safe route not found: " + safeRouteId));

        List<NearbyFacilityResponse> facilities = googlePlacesService.findNearbySafetyFacilities(
                route.getOriginLatitude(), route.getOriginLongitude(), 2500
        );

        RouteSafetyScoreService.SafetyEvaluation eval = safetyScoreService.evaluateRouteSafety(
                route.getOriginLatitude(), route.getOriginLongitude(),
                route.getDestinationLatitude(), route.getDestinationLongitude(),
                route.getTravelMode(), route.getSafeRouteId(), facilities
        );

        SafeRouteResponse response = SafeRouteResponse.fromEntity(route);
        response.setFactors(eval.getFactors());
        response.setNearbyFacilities(facilities);
        response.setWarnings(eval.getWarnings());
        return response;
    }

    /**
     * Submits or updates a citizen star rating and safety comment for a route.
     * Enforces single rating per citizen per route.
     */
    @Transactional
    public RouteRatingResponse addRouteRating(String safeRouteId, Long citizenId, RouteRatingRequest request) {
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }
        if (safeRouteId == null || safeRouteId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Safe route ID is required");
        }
        if (request == null || request.getRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating value (1-5) is required");
        }

        SafeRoute route = safeRouteRepository.findBySafeRouteId(safeRouteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Safe route not found: " + safeRouteId));

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Citizen user not found"));

        // Check if citizen has already rated this route
        SafeRouteRating rating = ratingRepository.findByRouteIdAndCitizenId(route.getId(), citizenId)
                .orElseGet(() -> new SafeRouteRating(route, citizen, request.getRating(), request.getComment(), request.getSafetyTag()));

        rating.setRating(request.getRating());
        rating.setComment(request.getComment() != null ? request.getComment().trim() : null);
        rating.setSafetyTag(request.getSafetyTag() != null ? request.getSafetyTag().trim() : null);

        SafeRouteRating savedRating = ratingRepository.save(rating);

        // Recalculate route rating metrics
        Double newAvg = ratingRepository.getAverageRatingForRoute(route.getId());
        long newCount = ratingRepository.countByRouteId(route.getId());

        route.setAverageRating(newAvg != null ? Math.round(newAvg * 10.0) / 10.0 : 0.0);
        route.setRatingCount((int) newCount);

        // Re-evaluate safety score with new ratings
        RouteSafetyScoreService.SafetyEvaluation eval = safetyScoreService.evaluateRouteSafety(
                route.getOriginLatitude(), route.getOriginLongitude(),
                route.getDestinationLatitude(), route.getDestinationLongitude(),
                route.getTravelMode(), route.getSafeRouteId(), null
        );
        route.setSafetyScore(eval.getSafetyScore());
        route.setSafetyLevel(eval.getSafetyLevel());
        route.setDataConfidence(eval.getDataConfidence());

        safeRouteRepository.save(route);

        // Check auto-file trigger
        autoGrievanceService.checkAndAutoFileGrievance(route, citizenId);

        return RouteRatingResponse.fromEntity(savedRating);
    }

    /**
     * Lists all public ratings for a given route.
     */
    @Transactional(readOnly = true)
    public List<RouteRatingResponse> getRouteRatings(String safeRouteId) {
        if (safeRouteId == null || safeRouteId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Safe route ID is required");
        }
        return ratingRepository.findByRouteSafeRouteIdOrderByCreatedAtDesc(safeRouteId).stream()
                .map(RouteRatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Admin: Lists all analyzed safe routes, newest first.
     */
    @Transactional(readOnly = true)
    public List<SafeRouteResponse> getAllRoutesForAdmin() {
        return safeRouteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SafeRouteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Admin: Retrieves high-risk routes with safety score <= threshold.
     */
    @Transactional(readOnly = true)
    public List<SafeRouteResponse> getHighRiskRoutesForAdmin(Integer threshold) {
        int th = (threshold != null) ? threshold : 40;
        return safeRouteRepository.findBySafetyScoreLessThanEqualOrderBySafetyScoreAsc(th).stream()
                .map(SafeRouteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Admin: Returns high-level metrics for routes and safety scores.
     */
    @Transactional(readOnly = true)
    public AdminRouteSummaryResponse getAdminSummary() {
        long totalRoutes = safeRouteRepository.count();
        long highRiskCount = safeRouteRepository.countBySafetyScoreLessThanEqual(39);
        long autoFiled = safeRouteRepository.countByAutoGrievanceFiledTrue();
        long totalRatings = ratingRepository.count();

        double avgScore = 0.0;
        List<SafeRoute> all = safeRouteRepository.findAll();
        if (!all.isEmpty()) {
            double sum = all.stream().mapToInt(SafeRoute::getSafetyScore).sum();
            avgScore = Math.round((sum / all.size()) * 10.0) / 10.0;
        }

        return new AdminRouteSummaryResponse(totalRoutes, avgScore, highRiskCount, totalRatings, autoFiled);
    }

    /**
     * Public config status for Safe Routes frontend.
     */
    public SafeRouteConfigResponse getPublicConfig() {
        boolean routesConfigured = googleRoutesService.isConfigured();
        boolean placesConfigured = googlePlacesService.isConfigured();
        boolean mapsJsConfigured = mapsBrowserApiKey != null && !mapsBrowserApiKey.isBlank();

        String status;
        if (routesConfigured && mapsJsConfigured) {
            status = "Google Maps & Routes services fully operational.";
        } else if (mapsJsConfigured) {
            status = "Google Maps JS active. Routes running in baseline safety geometry.";
        } else {
            status = "Google APIs unconfigured. Safe Routes operating in free OpenStreetMap / Leaflet mode.";
        }

        return new SafeRouteConfigResponse(
                routesConfigured,
                placesConfigured,
                mapsJsConfigured,
                mapsBrowserApiKey,
                status
        );
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
