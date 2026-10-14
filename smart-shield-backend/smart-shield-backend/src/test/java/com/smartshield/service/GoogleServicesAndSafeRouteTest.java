package com.smartshield.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartshield.controller.SafeRouteController;
import com.smartshield.dto.DestinationSuggestionDto;
import com.smartshield.dto.LocationResolutionResponse;
import com.smartshield.dto.NearbyFacilityResponse;
import com.smartshield.dto.SafeRouteConfigResponse;
import com.smartshield.dto.SafeRouteRequest;
import com.smartshield.dto.SafeRouteResponse;
import com.smartshield.dto.SafetyFactorResponse;
import com.smartshield.model.SafeRoute;
import com.smartshield.repository.SafeRouteRatingRepository;
import com.smartshield.repository.SafeRouteRepository;
import com.smartshield.repository.UserRepository;
import com.smartshield.util.RateLimiterUtil;

@ExtendWith(MockitoExtension.class)
class GoogleServicesAndSafeRouteTest {

    @Mock
    private SafeRouteRepository safeRouteRepository;

    @Mock
    private SafeRouteRatingRepository ratingRepository;

    @Mock
    private UserRepository userRepository;

    private LocationService locationService;
    private GoogleRoutesService googleRoutesService;
    private GooglePlacesService googlePlacesService;
    private SafeRouteService safeRouteService;

    // Subclass to avoid ByteBuddy bytecode instrumentation restrictions on JDK 25
    static class StubSafetyScoreService extends RouteSafetyScoreService {
        public StubSafetyScoreService() {
            super(null, null, null);
        }

        @Override
        public SafetyEvaluation evaluateRouteSafety(Double originLat, Double originLng,
                                                    Double destLat, Double destLng,
                                                    String originAddress, String destinationAddress,
                                                    String travelMode, String routeId,
                                                    List<NearbyFacilityResponse> facilities) {
            return new SafetyEvaluation(
                    0, "INSUFFICIENT_DATA", "LOW",
                    List.of(new SafetyFactorResponse("Verified Records", "NEUTRAL", 0, "No data")),
                    List.of("Notice: Server running with baseline route geometry.")
            );
        }
    }

    static class StubAutoGrievanceService extends AutoGrievanceService {
        public StubAutoGrievanceService() {
            super(null, null, null, null);
        }

        @Override
        public boolean checkAndAutoFileGrievance(SafeRoute route, Long citizenId) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        locationService = new LocationService();
        googleRoutesService = new GoogleRoutesService();
        googlePlacesService = new GooglePlacesService();
        RateLimiterUtil rateLimiterUtil = new RateLimiterUtil();

        safeRouteService = new SafeRouteService(
                safeRouteRepository,
                ratingRepository,
                userRepository,
                googleRoutesService,
                googlePlacesService,
                new StubSafetyScoreService(),
                new StubAutoGrievanceService(),
                locationService,
                rateLimiterUtil
        );
    }

    @Test
    void testGoogleRoutesUnconfiguredByDefault() {
        assertFalse(googleRoutesService.isConfigured(), "Google Routes must report NOT configured when API key is empty");
        Optional<GoogleRoutesService.GoogleRouteData> result =
                googleRoutesService.computeRoute(28.6692, 77.4538, 28.6672, 77.4328, "WALK");
        assertTrue(result.isEmpty(), "Must return empty Optional without crashing when not configured");
    }

    @Test
    void testGooglePlacesUnconfiguredByDefault() {
        assertFalse(googlePlacesService.isConfigured(), "Google Places must report NOT configured when API key is empty");
        List<?> facilities = googlePlacesService.findNearbySafetyFacilities(28.6692, 77.4538, 2000);
        assertTrue(facilities.isEmpty(), "Must return empty facilities without fabricating places");

        List<DestinationSuggestionDto> places = googlePlacesService.searchPlaces("hospital", "Ghaziabad");
        assertTrue(places.isEmpty(), "Must return empty suggestions without fabricating results");
    }

    @Test
    void testPublicConfigHonestyAndZeroKeyLeakage() {
        SafeRouteConfigResponse config = safeRouteService.getPublicConfig();
        assertNotNull(config);
        assertFalse(config.isGoogleRoutesConfigured(), "Routes must honestly be false");
        assertFalse(config.isGooglePlacesConfigured(), "Places must honestly be false");
        assertFalse(config.isGoogleMapsJsConfigured(), "Browser Maps JS must honestly be false");
        assertTrue(config.getStatusMessage().contains("local baseline mode") || config.getStatusMessage().contains("unconfigured"));
    }

    @Test
    void testCalculateRouteWithBaselineFallbackGeometry() {
        when(safeRouteRepository.save(any(SafeRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SafeRouteRequest req = new SafeRouteRequest();
        req.setOriginLatitude(28.6692);
        req.setOriginLongitude(77.4538);
        req.setOriginAddress("Model Town");
        req.setDestinationLatitude(28.6672);
        req.setDestinationLongitude(77.4328);
        req.setDestinationAddress("MMG Hospital");
        req.setTravelMode("WALK");

        SafeRouteResponse response = safeRouteService.calculateSafeRoute(1L, req, "127.0.0.1");

        assertNotNull(response);
        assertEquals("INSUFFICIENT_DATA", response.getSafetyLevel(), "Zero evidence must produce INSUFFICIENT_DATA");
        assertEquals(0, response.getSafetyScore());
        assertTrue(response.getDistanceMeters() > 0, "Distance must be calculated via baseline Haversine");
        assertTrue(response.getDurationSeconds() > 0, "Duration must be calculated via walking velocity");
        assertNotNull(response.getWarnings());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("baseline route geometry")));
    }

    @Test
    void testLocationResolutionLegitimateBoundaries() {
        // Test legitimate coordinates inside Ghaziabad polygon
        LocationResolutionResponse gz = locationService.resolveLocation(28.6672, 77.4328);
        assertTrue(gz.isResolved());
        assertEquals("Ghaziabad", gz.getCity());
        assertEquals("Uttar Pradesh", gz.getState());
        assertFalse(gz.getNearbyDestinations().isEmpty());

        // Test coordinates in the Pacific Ocean (must NOT fabricate fake city)
        LocationResolutionResponse ocean = locationService.resolveLocation(0.0, 160.0);
        assertFalse(ocean.isResolved(), "Coordinates outside verified regions must return unresolved");
        org.junit.jupiter.api.Assertions.assertNull(ocean.getCity(), "Unresolved city must be null");
    }

    @Test
    void testSafeRouteControllerDestinationFallback() {
        SafeRouteController controller = new SafeRouteController(safeRouteService, locationService, googlePlacesService);
        var res = controller.getDestinations("Ghaziabad", "Hospital");
        assertNotNull(res.getBody());
        assertFalse(res.getBody().isEmpty(), "Must return verified municipal hospital from local boundary database");
        assertTrue(res.getBody().stream().anyMatch(d -> d.getName().toLowerCase().contains("hospital")));
    }
}
