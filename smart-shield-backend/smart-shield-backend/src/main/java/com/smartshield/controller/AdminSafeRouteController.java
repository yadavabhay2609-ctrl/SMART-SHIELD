package com.smartshield.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartshield.dto.AdminRouteSummaryResponse;
import com.smartshield.dto.RouteRatingResponse;
import com.smartshield.dto.SafeRouteResponse;
import com.smartshield.service.SafeRouteService;

/**
 * REST controller for Administrative Safe Route monitoring and high-risk audits.
 * Automatically protected by AdminAuthInterceptor under /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/safety/routes")
public class AdminSafeRouteController {

    private final SafeRouteService safeRouteService;

    public AdminSafeRouteController(SafeRouteService safeRouteService) {
        this.safeRouteService = safeRouteService;
    }

    /**
     * 1. GET /api/admin/safety/routes
     * Lists all analyzed safe routes, newest first.
     */
    @GetMapping
    public ResponseEntity<List<SafeRouteResponse>> getAllRoutes() {
        List<SafeRouteResponse> routes = safeRouteService.getAllRoutesForAdmin();
        return ResponseEntity.ok(routes);
    }

    /**
     * 2. GET /api/admin/safety/routes/summary
     * Returns aggregate high-level metrics for routes, safety scores, and auto-filed grievances.
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminRouteSummaryResponse> getSummary() {
        AdminRouteSummaryResponse summary = safeRouteService.getAdminSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * 3. GET /api/admin/safety/routes/high-risk
     * Lists all routes with safety score below or equal to threshold (default 39).
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<SafeRouteResponse>> getHighRiskRoutes(
            @RequestParam(required = false, defaultValue = "39") Integer threshold) {
        List<SafeRouteResponse> routes = safeRouteService.getHighRiskRoutesForAdmin(threshold);
        return ResponseEntity.ok(routes);
    }

    /**
     * 4. GET /api/admin/safety/routes/{safeRouteId}
     * Retrieves specific route details for administrative inspection.
     */
    @GetMapping("/{safeRouteId}")
    public ResponseEntity<SafeRouteResponse> getRoute(@PathVariable String safeRouteId) {
        SafeRouteResponse response = safeRouteService.getSafeRoute(safeRouteId);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. GET /api/admin/safety/routes/{safeRouteId}/ratings
     * Retrieves all citizen ratings and reviews for administrative review.
     */
    @GetMapping("/{safeRouteId}/ratings")
    public ResponseEntity<List<RouteRatingResponse>> getRatings(@PathVariable String safeRouteId) {
        List<RouteRatingResponse> ratings = safeRouteService.getRouteRatings(safeRouteId);
        return ResponseEntity.ok(ratings);
    }
}
