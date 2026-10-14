package com.smartshield.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartshield.dto.NearbyFacilityResponse;
import com.smartshield.dto.SafetyFactorResponse;
import com.smartshield.model.Grievance;
import com.smartshield.model.GrievanceCategory;
import com.smartshield.model.SosAlert;
import com.smartshield.repository.GrievanceRepository;
import com.smartshield.repository.SafeRouteRatingRepository;
import com.smartshield.repository.SosAlertRepository;

/**
 * Core business engine for transparent, weighted Route Safety Scoring.
 * Computes an explainable 0-100 safety score, safety level classification,
 * and data confidence level from verified Smart Shield database signals.
 */
@Service
public class RouteSafetyScoreService {

    private final GrievanceRepository grievanceRepository;
    private final SosAlertRepository sosAlertRepository;
    private final SafeRouteRatingRepository ratingRepository;

    public RouteSafetyScoreService(GrievanceRepository grievanceRepository,
                                  SosAlertRepository sosAlertRepository,
                                  SafeRouteRatingRepository ratingRepository) {
        this.grievanceRepository = grievanceRepository;
        this.sosAlertRepository = sosAlertRepository;
        this.ratingRepository = ratingRepository;
    }

    public static class SafetyEvaluation {
        private final int safetyScore;
        private final String safetyLevel;
        private final String dataConfidence;
        private final List<SafetyFactorResponse> factors;
        private final List<String> warnings;

        public SafetyEvaluation(int safetyScore, String safetyLevel, String dataConfidence,
                                List<SafetyFactorResponse> factors, List<String> warnings) {
            this.safetyScore = safetyScore;
            this.safetyLevel = safetyLevel;
            this.dataConfidence = dataConfidence;
            this.factors = factors;
            this.warnings = warnings;
        }

        public int getSafetyScore() {
            return safetyScore;
        }

        public String getSafetyLevel() {
            return safetyLevel;
        }

        public String getDataConfidence() {
            return dataConfidence;
        }

        public List<SafetyFactorResponse> getFactors() {
            return factors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    /**
     * Calculates transparent safety score and factors for a route corridor.
     */
    public SafetyEvaluation evaluateRouteSafety(Double originLat, Double originLng,
                                               Double destLat, Double destLng,
                                               String travelMode, String routeId,
                                               List<NearbyFacilityResponse> facilities) {
        return evaluateRouteSafety(originLat, originLng, destLat, destLng, null, null, travelMode, routeId, facilities);
    }

    /**
     * Calculates transparent safety score and factors for a route corridor with localized address context.
     */
    public SafetyEvaluation evaluateRouteSafety(Double originLat, Double originLng,
                                               Double destLat, Double destLng,
                                               String originAddress, String destinationAddress,
                                               String travelMode, String routeId,
                                               List<NearbyFacilityResponse> facilities) {
        List<SafetyFactorResponse> factors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        double minLat = Math.min(originLat, destLat) - 0.015;
        double maxLat = Math.max(originLat, destLat) + 0.015;
        double minLng = Math.min(originLng, destLng) - 0.015;
        double maxLng = Math.max(originLng, destLng) + 0.015;

        // 1. Historical SOS Emergency Density (Anonymized aggregate corridor risk)
        List<SosAlert> allAlerts = sosAlertRepository.findAll();
        int areaSosCount = 0;
        for (SosAlert alert : allAlerts) {
            if (alert.getLatitude() != null && alert.getLongitude() != null) {
                if (alert.getLatitude() >= minLat && alert.getLatitude() <= maxLat &&
                    alert.getLongitude() >= minLng && alert.getLongitude() <= maxLng) {
                    areaSosCount++;
                }
            }
        }

        // 2. Public Safety Grievances (Corridor-localized check)
        List<Grievance> allGrievances = grievanceRepository.findAll();
        int safetyGrievanceCount = 0;
        String combinedAddresses = ((originAddress != null ? originAddress : "") + " " +
                                    (destinationAddress != null ? destinationAddress : "")).toLowerCase();

        for (Grievance g : allGrievances) {
            if (g.getCategory() == GrievanceCategory.PUBLIC_SAFETY ||
                g.getCategory() == GrievanceCategory.ELECTRICITY) {
                // Check if grievance location matches route corridor keywords
                String gLoc = (g.getLocation() != null ? g.getLocation() : "").toLowerCase();
                if (!gLoc.isBlank() && !combinedAddresses.isBlank()) {
                    // Match city or landmark tokens, ignoring generic address stop words
                    java.util.Set<String> stopWords = java.util.Set.of(
                            "sector", "block", "road", "street", "lane", "nagar", "place", "point",
                            "near", "area", "city", "west", "east", "north", "south", "central",
                            "main", "phase", "rural", "urban", "district", "outer", "inner", "circle"
                    );
                    String[] tokens = gLoc.split("[\\s,]+");
                    boolean matched = false;
                    for (String token : tokens) {
                        String t = token.toLowerCase();
                        if (t.length() >= 4 && !stopWords.contains(t) && combinedAddresses.contains(t)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        safetyGrievanceCount++;
                    }
                }
            }
        }

        // 3. Citizen Route Ratings (if existing route has reviews)
        Double avgRating = null;
        long ratingCount = 0;
        if (routeId != null && !routeId.isBlank()) {
            avgRating = ratingRepository.getAverageRatingForSafeRouteId(routeId);
            ratingCount = ratingRepository.countByRouteSafeRouteId(routeId);
        }

        int verifiedEvidenceSignals = areaSosCount + safetyGrievanceCount + (int) ratingCount;
        int facilityCount = (facilities != null) ? facilities.size() : 0;

        // Requirement 11: ZERO-EVIDENCE ROUTE BEHAVIOR
        // If a route has 0 ratings, 0 SOS incidents, 0 grievance reports, and 0 facility signals:
        // Do NOT claim "High Risk" or "Unsafe". Show INSUFFICIENT_DATA.
        if (verifiedEvidenceSignals == 0 && facilityCount == 0) {
            factors.add(new SafetyFactorResponse(
                    "Verified Community Records",
                    "NEUTRAL",
                    0,
                    "No verified citizen safety reports, emergency incidents, or community reviews are currently available for this route corridor."
            ));
            warnings.add("Safety Status: Insufficient community data. Exercise standard situational awareness.");
            return new SafetyEvaluation(0, "INSUFFICIENT_DATA", "LOW", factors, warnings);
        }

        // Baseline score for routes with available civic signals
        int score = 75;

        // Process SOS Density
        if (areaSosCount == 0) {
            factors.add(new SafetyFactorResponse(
                    "Emergency SOS Density",
                    "POSITIVE",
                    +5,
                    "Zero emergency SOS incidents recorded along this transit corridor."
            ));
            score += 5;
        } else if (areaSosCount <= 2) {
            factors.add(new SafetyFactorResponse(
                    "Emergency SOS Density",
                    "NEUTRAL",
                    -5,
                    areaSosCount + " emergency SOS incident(s) reported in surrounding area."
            ));
            score -= 5;
            warnings.add("Caution: Isolated emergency SOS incidents recorded nearby.");
        } else {
            int penalty = Math.min(30, areaSosCount * 8);
            factors.add(new SafetyFactorResponse(
                    "Emergency SOS Density",
                    "NEGATIVE",
                    -penalty,
                    "Elevated frequency of emergency SOS alerts (" + areaSosCount + ") recorded in this sector."
            ));
            score -= penalty;
            warnings.add("Warning: Heightened emergency activity detected along this path.");
        }

        // Process Public Safety Grievances
        if (safetyGrievanceCount > 2) {
            int gPenalty = Math.min(20, (safetyGrievanceCount - 2) * 5);
            factors.add(new SafetyFactorResponse(
                    "Public Safety Grievances",
                    "NEGATIVE",
                    -gPenalty,
                    safetyGrievanceCount + " verified dark spot / public safety complaint(s) active in corridor."
            ));
            score -= gPenalty;
            warnings.add("Caution: Active civic safety complaints reported along corridor.");
        } else if (safetyGrievanceCount > 0) {
            factors.add(new SafetyFactorResponse(
                    "Public Safety Grievances",
                    "NEUTRAL",
                    -3,
                    safetyGrievanceCount + " civic maintenance issue(s) reported nearby."
            ));
            score -= 3;
        } else {
            factors.add(new SafetyFactorResponse(
                    "Public Safety Grievances",
                    "POSITIVE",
                    +5,
                    "Zero active dark spot or public safety grievances in this sector."
            ));
            score += 5;
        }

        // Process Community Ratings
        if (avgRating != null && ratingCount > 0) {
            if (avgRating >= 4.0) {
                int boost = (int) Math.round((avgRating - 3.0) * 8);
                factors.add(new SafetyFactorResponse(
                        "Citizen Community Ratings",
                        "POSITIVE",
                        +boost,
                        String.format("Highly rated by citizens (%.1f/5 stars, %d reviews).", avgRating, ratingCount)
                ));
                score += boost;
            } else if (avgRating < 2.5) {
                int drop = (int) Math.round((3.0 - avgRating) * 12);
                factors.add(new SafetyFactorResponse(
                        "Citizen Community Ratings",
                        "NEGATIVE",
                        -drop,
                        String.format("Low citizen ratings reported (%.1f/5 stars, %d reviews).", avgRating, ratingCount)
                ));
                score -= drop;
                warnings.add("Community feedback indicates safety concerns along this route.");
            } else {
                factors.add(new SafetyFactorResponse(
                        "Citizen Community Ratings",
                        "NEUTRAL",
                        0,
                        String.format("Moderate community rating (%.1f/5 stars, %d reviews).", avgRating, ratingCount)
                ));
            }
        }

        // Process Emergency Infrastructure
        if (facilityCount > 0) {
            int facilityBoost = Math.min(15, facilityCount * 5);
            factors.add(new SafetyFactorResponse(
                    "Emergency Infrastructure",
                    "POSITIVE",
                    +facilityBoost,
                    facilityCount + " emergency facility(ies) (Police/Hospital) accessible along route."
            ));
            score += facilityBoost;
        }

        // Night Travel Check (10 PM to 5 AM)
        LocalTime now = LocalTime.now();
        boolean isNightTime = now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(5, 0));
        if ("WALK".equalsIgnoreCase(travelMode) && isNightTime) {
            factors.add(new SafetyFactorResponse(
                    "Night Walk Assessment",
                    "NEGATIVE",
                    -10,
                    "Night-time pedestrian transit: reduced visibility and isolated transit windows."
            ));
            score -= 10;
            warnings.add("Night travel warning: Stick to well-lit main thoroughfares and keep emergency contacts ready.");
        }

        // Clamp final score to 0 - 100
        int finalScore = Math.max(0, Math.min(100, score));

        // Determine Safety Level
        String safetyLevel;
        if (finalScore >= 80) {
            safetyLevel = "VERY_SAFE";
        } else if (finalScore >= 60) {
            safetyLevel = "SAFE";
        } else if (finalScore >= 40) {
            safetyLevel = "MODERATE";
        } else if (finalScore >= 20) {
            safetyLevel = "RISKY";
        } else {
            safetyLevel = "HIGH_RISK";
        }

        int totalSignals = verifiedEvidenceSignals + facilityCount;
        String dataConfidence;
        if (totalSignals >= 5) {
            dataConfidence = "HIGH";
        } else if (totalSignals >= 2) {
            dataConfidence = "MEDIUM";
        } else {
            dataConfidence = "LOW";
        }

        return new SafetyEvaluation(finalScore, safetyLevel, dataConfidence, factors, warnings);
    }
}
