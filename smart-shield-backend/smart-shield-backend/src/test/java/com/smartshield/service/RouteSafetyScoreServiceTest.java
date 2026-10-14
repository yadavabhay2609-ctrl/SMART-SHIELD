package com.smartshield.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartshield.repository.GrievanceRepository;
import com.smartshield.repository.SafeRouteRatingRepository;
import com.smartshield.repository.SosAlertRepository;

@ExtendWith(MockitoExtension.class)
class RouteSafetyScoreServiceTest {

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private SosAlertRepository sosAlertRepository;

    @Mock
    private SafeRouteRatingRepository ratingRepository;

    private RouteSafetyScoreService service;

    @BeforeEach
    void setUp() {
        service = new RouteSafetyScoreService(grievanceRepository, sosAlertRepository, ratingRepository);
    }

    @Test
    void testEvaluateRouteSafetyWithZeroIncidents() {
        when(sosAlertRepository.findAll()).thenReturn(Collections.emptyList());
        when(grievanceRepository.findAll()).thenReturn(Collections.emptyList());

        RouteSafetyScoreService.SafetyEvaluation eval = service.evaluateRouteSafety(
                28.6139, 77.2090,
                28.6280, 77.2180,
                "WALK",
                null,
                List.of()
        );

        assertNotNull(eval);
        assertTrue(eval.getSafetyScore() >= 0 && eval.getSafetyScore() <= 100);
        org.junit.jupiter.api.Assertions.assertEquals("INSUFFICIENT_DATA", eval.getSafetyLevel());
        org.junit.jupiter.api.Assertions.assertEquals("LOW", eval.getDataConfidence());
        assertNotNull(eval.getFactors());
        assertTrue(eval.getFactors().size() > 0);
    }

    @Test
    void testEvaluateRouteSafetyWithEvidence() {
        when(sosAlertRepository.findAll()).thenReturn(Collections.emptyList());
        when(grievanceRepository.findAll()).thenReturn(Collections.emptyList());

        com.smartshield.dto.NearbyFacilityResponse facility = new com.smartshield.dto.NearbyFacilityResponse(
                "Central Police Station", "POLICE", 28.6150, 77.2100, 200
        );

        RouteSafetyScoreService.SafetyEvaluation eval = service.evaluateRouteSafety(
                28.6139, 77.2090,
                28.6280, 77.2180,
                "WALK",
                null,
                List.of(facility)
        );

        assertNotNull(eval);
        assertTrue(eval.getSafetyScore() > 0);
        assertTrue(!"INSUFFICIENT_DATA".equals(eval.getSafetyLevel()));
        assertNotNull(eval.getDataConfidence());
    }
}
