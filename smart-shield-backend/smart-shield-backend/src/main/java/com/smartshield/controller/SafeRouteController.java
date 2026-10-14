package com.smartshield.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.DestinationSuggestionDto;
import com.smartshield.dto.LocationResolutionResponse;
import com.smartshield.dto.RouteRatingRequest;
import com.smartshield.dto.RouteRatingResponse;
import com.smartshield.dto.SafeRouteConfigResponse;
import com.smartshield.dto.SafeRouteRequest;
import com.smartshield.dto.SafeRouteResponse;
import com.smartshield.model.User;
import com.smartshield.service.GooglePlacesService;
import com.smartshield.service.LocationService;
import com.smartshield.service.SafeRouteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for Citizen Safe Routes operations.
 * Protected by CitizenAuthInterceptor under /api/safety/**.
 */
@RestController
@RequestMapping("/api/safety/routes")
public class SafeRouteController {

    private final SafeRouteService safeRouteService;
    private final LocationService locationService;
    private final GooglePlacesService googlePlacesService;

    public SafeRouteController(SafeRouteService safeRouteService,
                               LocationService locationService,
                               GooglePlacesService googlePlacesService) {
        this.safeRouteService = safeRouteService;
        this.locationService = locationService;
        this.googlePlacesService = googlePlacesService;
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
     * 1. GET /api/safety/routes/config
     * Returns public configuration status of Google services for Safe Routes frontend.
     */
    @GetMapping("/config")
    public ResponseEntity<SafeRouteConfigResponse> getConfig() {
        SafeRouteConfigResponse response = safeRouteService.getPublicConfig();
        return ResponseEntity.ok(response);
    }

    /**
     * Resolves browser GPS coordinates to verified administrative city/state data
     * and returns curated same-city destinations.
     */
    @GetMapping("/resolve-location")
    public ResponseEntity<LocationResolutionResponse> resolveLocation(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        LocationResolutionResponse response = locationService.resolveLocation(lat, lng);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns destination suggestions matching an optional query.
     * Uses live Google Places API (New) when configured, or verified municipal registry.
     */
    @GetMapping("/destinations")
    public ResponseEntity<List<DestinationSuggestionDto>> getDestinations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        if (query != null && !query.isBlank() && googlePlacesService.isConfigured()) {
            List<DestinationSuggestionDto> livePlaces = googlePlacesService.searchPlaces(query, city);
            if (!livePlaces.isEmpty()) {
                return ResponseEntity.ok(livePlaces);
            }
        }
        if ((city == null || city.isBlank()) && lat != null && lng != null) {
            for (LocationService.CityBoundary boundary : locationService.getVerifiedBoundaries()) {
                if (boundary.contains(lat, lng)) {
                    city = boundary.getCity();
                    break;
                }
            }
        }
        List<DestinationSuggestionDto> destinations = locationService.getDestinationsForCity(city, query);
        return ResponseEntity.ok(destinations);
    }

    public ResponseEntity<List<DestinationSuggestionDto>> getDestinations(String city, String query) {
        return getDestinations(city, query, null, null);
    }

    /**
     * 2. POST /api/safety/routes/calculate
     * Computes an evaluated safe route between two coordinates with transparent safety score.
     */
    @PostMapping("/calculate")
    public ResponseEntity<SafeRouteResponse> calculateRoute(
            @Valid @RequestBody SafeRouteRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        String clientIp = resolveClientIp(httpRequest);

        SafeRouteResponse response = safeRouteService.calculateSafeRoute(citizenId, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3. GET /api/safety/routes/{safeRouteId}
     * Retrieves details and safety factors for an existing safe route.
     */
    @GetMapping("/{safeRouteId}")
    public ResponseEntity<SafeRouteResponse> getRoute(@PathVariable String safeRouteId) {
        SafeRouteResponse response = safeRouteService.getSafeRoute(safeRouteId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. POST /api/safety/routes/{safeRouteId}/rating
     * Submits or updates a citizen star rating and comment for a route.
     */
    @PostMapping("/{safeRouteId}/rating")
    public ResponseEntity<RouteRatingResponse> submitRating(
            @PathVariable String safeRouteId,
            @Valid @RequestBody RouteRatingRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        RouteRatingResponse response = safeRouteService.addRouteRating(safeRouteId, citizenId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. GET /api/safety/routes/{safeRouteId}/ratings
     * Retrieves all public citizen ratings and reviews for a safe route.
     */
    @GetMapping("/{safeRouteId}/ratings")
    public ResponseEntity<List<RouteRatingResponse>> getRatings(@PathVariable String safeRouteId) {
        List<RouteRatingResponse> ratings = safeRouteService.getRouteRatings(safeRouteId);
        return ResponseEntity.ok(ratings);
    }
}
