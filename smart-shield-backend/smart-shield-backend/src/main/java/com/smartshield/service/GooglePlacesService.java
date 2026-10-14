package com.smartshield.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.smartshield.dto.DestinationSuggestionDto;
import com.smartshield.dto.NearbyFacilityResponse;

/**
 * Service integrating with the official Google Places API (New) to identify nearby
 * public safety infrastructure (police stations, hospitals) and search destinations.
 */
@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);

    private static final String GOOGLE_PLACES_NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String GOOGLE_PLACES_TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";

    @Value("${smartshield.google.places.enabled:false}")
    private boolean placesEnabled;

    @Value("${smartshield.google.places.api-key:}")
    private String placesApiKey;

    private final RestTemplate restTemplate;

    public GooglePlacesService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return placesEnabled && placesApiKey != null && !placesApiKey.isBlank();
    }

    /**
     * Searches for nearby police stations and hospitals within a specified radius (meters)
     * around a given midpoint or origin coordinate.
     */
    @SuppressWarnings("unchecked")
    public List<NearbyFacilityResponse> findNearbySafetyFacilities(Double lat, Double lng, double radiusMeters) {
        if (!isConfigured() || lat == null || lng == null) {
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", placesApiKey);
            headers.set("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location,places.types");
            headers.set("X-Goog-Maps-Solution-ID", "gmp_git_agentskills_v1");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("includedTypes", List.of("police", "hospital"));
            requestBody.put("maxResultCount", 5);
            requestBody.put("locationRestriction", Map.of(
                    "circle", Map.of(
                            "center", Map.of("latitude", lat, "longitude", lng),
                            "radius", Math.min(radiusMeters, 5000.0)
                    )
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_PLACES_NEARBY_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> places = (List<Map<String, Object>>) response.getBody().get("places");
                if (places != null) {
                    List<NearbyFacilityResponse> result = new ArrayList<>();
                    for (Map<String, Object> p : places) {
                        Map<String, Object> displayNameMap = (Map<String, Object>) p.get("displayName");
                        String name = displayNameMap != null ? (String) displayNameMap.get("text") : "Emergency Service";

                        List<String> types = (List<String>) p.get("types");
                        String type = "EMERGENCY";
                        if (types != null) {
                            if (types.contains("police")) type = "POLICE";
                            else if (types.contains("hospital")) type = "HOSPITAL";
                        }

                        Map<String, Object> location = (Map<String, Object>) p.get("location");
                        Double pLat = location != null ? (Double) location.get("latitude") : lat;
                        Double pLng = location != null ? (Double) location.get("longitude") : lng;

                        int distanceMeters = (int) calculateHaversineDistance(lat, lng, pLat, pLng);

                        result.add(new NearbyFacilityResponse(name, type, pLat, pLng, distanceMeters));
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Google Places API query failed: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Searches for destinations matching query text using Google Places API (New) Text Search.
     * Returns mapped DestinationSuggestionDto list with genuine coordinates and formatted addresses.
     */
    @SuppressWarnings("unchecked")
    public List<DestinationSuggestionDto> searchPlaces(String query, String city) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", placesApiKey);
            headers.set("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location,places.types");
            headers.set("X-Goog-Maps-Solution-ID", "gmp_git_agentskills_v1");

            String fullTextQuery = query.trim();
            if (city != null && !city.isBlank() && !fullTextQuery.toLowerCase().contains(city.toLowerCase())) {
                fullTextQuery += ", " + city.trim();
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("textQuery", fullTextQuery);
            requestBody.put("maxResultCount", 6);
            requestBody.put("languageCode", "en");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_PLACES_TEXT_SEARCH_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> places = (List<Map<String, Object>>) response.getBody().get("places");
                if (places != null && !places.isEmpty()) {
                    List<DestinationSuggestionDto> result = new ArrayList<>();
                    for (Map<String, Object> p : places) {
                        Map<String, Object> displayNameMap = (Map<String, Object>) p.get("displayName");
                        String name = displayNameMap != null ? (String) displayNameMap.get("text") : "Destination";

                        String formattedAddress = (String) p.get("formattedAddress");
                        if (formattedAddress == null) {
                            formattedAddress = "";
                        }

                        List<String> types = (List<String>) p.get("types");
                        String category = "PUBLIC_PLACE";
                        if (types != null) {
                            if (types.contains("police")) category = "POLICE";
                            else if (types.contains("hospital")) category = "HOSPITAL";
                            else if (types.contains("subway_station") || types.contains("transit_station")) category = "METRO";
                            else if (types.contains("train_station") || types.contains("bus_station")) category = "TRANSIT";
                            else if (types.contains("shopping_mall") || types.contains("store")) category = "COMMERCIAL";
                            else if (types.contains("local_government_office")) category = "GOVT";
                            else if (types.contains("school") || types.contains("university")) category = "EDUCATION";
                        }

                        Map<String, Object> location = (Map<String, Object>) p.get("location");
                        if (location != null && location.get("latitude") != null && location.get("longitude") != null) {
                            Double lat = ((Number) location.get("latitude")).doubleValue();
                            Double lng = ((Number) location.get("longitude")).doubleValue();
                            result.add(new DestinationSuggestionDto(name, category, formattedAddress, city != null ? city : "", "", lat, lng));
                        }
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Google Places text search failed: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of Earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
