package com.smartshield.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service integrating with the official Google Routes API (v2:computeRoutes).
 * Safe placeholder handling: if API key is not configured, operations return empty/not-configured state
 * without crashing or fabricating fake external API responses.
 */
@Service
public class GoogleRoutesService {

    private static final Logger log = LoggerFactory.getLogger(GoogleRoutesService.class);

    private static final String GOOGLE_ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

    @Value("${smartshield.google.routes.enabled:false}")
    private boolean routesEnabled;

    @Value("${smartshield.google.routes.api-key:}")
    private String routesApiKey;

    private final RestTemplate restTemplate;

    public GoogleRoutesService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return routesEnabled && routesApiKey != null && !routesApiKey.isBlank();
    }

    /**
     * Calls Google Routes API to compute walking or driving directions between two coordinates.
     *
     * @param originLat origin latitude
     * @param originLng origin longitude
     * @param destLat destination latitude
     * @param destLng destination longitude
     * @param travelMode WALK or DRIVE
     * @return Optional containing route calculation result map, or empty if not configured or failed
     */
    @SuppressWarnings("unchecked")
    public Optional<GoogleRouteData> computeRoute(Double originLat, Double originLng,
                                                 Double destLat, Double destLng,
                                                 String travelMode) {
        if (!isConfigured()) {
            log.info("Google Routes API is not configured or disabled on this server. Proceeding with internal calculation.");
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", routesApiKey);
            headers.set("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.description,routes.warnings");
            headers.set("X-Goog-Maps-Solution-ID", "gmp_git_agentskills_v1");

            String googleTravelMode = "WALK".equalsIgnoreCase(travelMode) ? "WALK" : "DRIVE";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("origin", Map.of("location", Map.of("latLng", Map.of("latitude", originLat, "longitude", originLng))));
            requestBody.put("destination", Map.of("location", Map.of("latLng", Map.of("latitude", destLat, "longitude", destLng))));
            requestBody.put("travelMode", googleTravelMode);
            requestBody.put("computeAlternativeRoutes", false);
            requestBody.put("languageCode", "en-US");
            requestBody.put("units", "METRIC");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_ROUTES_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> primaryRoute = routes.get(0);

                    Integer distanceMeters = (Integer) primaryRoute.get("distanceMeters");
                    String durationStr = (String) primaryRoute.get("duration"); // e.g. "450s"
                    Integer durationSeconds = 0;
                    if (durationStr != null && durationStr.endsWith("s")) {
                        try {
                            String numPart = durationStr.substring(0, durationStr.length() - 1);
                            durationSeconds = (int) Math.round(Double.parseDouble(numPart));
                        } catch (NumberFormatException ignored) {}
                    }

                    Map<String, Object> polylineMap = (Map<String, Object>) primaryRoute.get("polyline");
                    String encodedPolyline = polylineMap != null ? (String) polylineMap.get("encodedPolyline") : null;
                    String description = (String) primaryRoute.get("description");
                    List<String> warnings = (List<String>) primaryRoute.get("warnings");

                    GoogleRouteData data = new GoogleRouteData(
                            distanceMeters != null ? distanceMeters : 0,
                            durationSeconds,
                            encodedPolyline,
                            description,
                            warnings != null ? warnings : Collections.emptyList()
                    );
                    return Optional.of(data);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query Google Routes API: {}", e.getMessage());
        }

        return Optional.empty();
    }

    public static class GoogleRouteData {
        private final int distanceMeters;
        private final int durationSeconds;
        private final String encodedPolyline;
        private final String description;
        private final List<String> warnings;

        public GoogleRouteData(int distanceMeters, int durationSeconds, String encodedPolyline,
                               String description, List<String> warnings) {
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.encodedPolyline = encodedPolyline;
            this.description = description;
            this.warnings = warnings;
        }

        public int getDistanceMeters() {
            return distanceMeters;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public String getEncodedPolyline() {
            return encodedPolyline;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
