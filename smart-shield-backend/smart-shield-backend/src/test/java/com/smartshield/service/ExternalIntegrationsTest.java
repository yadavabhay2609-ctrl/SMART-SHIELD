package com.smartshield.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartshield.controller.ConfigController;
import com.smartshield.dto.CreateGrievanceRequest;
import com.smartshield.dto.GrievanceResponse;
import com.smartshield.dto.PublicConfigResponse;
import com.smartshield.model.Grievance;
import com.smartshield.model.GrievanceCategory;
import com.smartshield.model.GrievancePriority;
import com.smartshield.model.GrievanceStatus;
import com.smartshield.model.GrievanceStatusHistory;
import com.smartshield.model.User;
import com.smartshield.repository.EmergencyContactRepository;
import com.smartshield.repository.GrievanceRepository;
import com.smartshield.repository.GrievanceStatusHistoryRepository;
import com.smartshield.repository.UserRepository;
import com.smartshield.util.TrackingIdGenerator;

@ExtendWith(MockitoExtension.class)
class ExternalIntegrationsTest {

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private GrievanceStatusHistoryRepository statusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    private TestSosNotificationService testSosNotificationService;
    private TestTelegramNotificationService testTelegramNotificationService;
    private TrackingIdGenerator trackingIdGenerator;
    private GrievanceService grievanceService;
    private GeminiService geminiService;

    // Subclasses to avoid ByteBuddy dynamic agent bytecode modification on JDK 25
    static class TestSosNotificationService extends SosNotificationService {
        boolean confirmationCalled = false;
        boolean statusUpdateCalled = false;

        public TestSosNotificationService() {
            super(null);
        }

        @Override
        public boolean sendGrievanceConfirmation(Grievance g) {
            confirmationCalled = true;
            return true;
        }

        @Override
        public boolean sendGrievanceStatusUpdate(Grievance g, GrievanceStatus o, GrievanceStatus n, String r) {
            statusUpdateCalled = true;
            return true;
        }
    }

    static class TestTelegramNotificationService extends TelegramNotificationService {
        boolean recvCalled = false;
        boolean updateCalled = false;

        public TestTelegramNotificationService(EmergencyContactRepository repo) {
            super(repo);
        }

        @Override
        public boolean sendGrievanceReceivedNotification(Grievance g) {
            recvCalled = true;
            return true;
        }

        @Override
        public boolean sendGrievanceStatusUpdatedNotification(Grievance g, GrievanceStatus o, GrievanceStatus n, String r) {
            updateCalled = true;
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        trackingIdGenerator = new TrackingIdGenerator();
        testSosNotificationService = new TestSosNotificationService();
        testTelegramNotificationService = new TestTelegramNotificationService(emergencyContactRepository);

        grievanceService = new GrievanceService(
                grievanceRepository,
                statusHistoryRepository,
                userRepository,
                trackingIdGenerator,
                testSosNotificationService,
                testTelegramNotificationService
        );
        geminiService = new GeminiService();
    }

    @Test
    void testConfigControllerResolution() {
        ConfigController controller = new ConfigController();
        ReflectionTestUtils.setField(controller, "mapsApiKey", "TEST_KEY_123");

        var response = controller.getConfig();
        assertNotNull(response);
        PublicConfigResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isMapsConfigured());
        assertEquals("TEST_KEY_123", body.getMapsApiKey());
    }

    @Test
    void testConfigControllerUnconfigured() {
        ConfigController controller = new ConfigController();
        ReflectionTestUtils.setField(controller, "mapsApiKey", "");

        var response = controller.getConfig();
        assertNotNull(response);
        PublicConfigResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isMapsConfigured());
        assertEquals("", body.getMapsApiKey());
    }

    @Test
    void testGeminiServiceUnconfiguredBehavior() {
        assertFalse(geminiService.isConfigured(), "Must report unconfigured when key is empty");
        assertEquals("gemini-1.5-flash", geminiService.getModelName());
        var answer = geminiService.generateAnswer("How to file road pothole complaint?", null);
        assertTrue(answer.isEmpty(), "Must return empty optional without throwing exception");
    }

    @Test
    void testTelegramNotificationServiceUnconfigured() {
        TelegramNotificationService telegramService = new TelegramNotificationService(emergencyContactRepository);
        assertFalse(telegramService.isConfigured());
        assertFalse(telegramService.isAuthorizedChatConfigured());
        assertFalse(telegramService.sendTestNotification(null));

        Grievance dummyGrievance = new Grievance();
        dummyGrievance.setTrackingId("SS-2026-TEST");
        assertFalse(telegramService.sendGrievanceReceivedNotification(dummyGrievance));
        assertFalse(telegramService.sendGrievanceStatusUpdatedNotification(dummyGrievance, GrievanceStatus.PENDING, GrievanceStatus.IN_REVIEW, "Crew dispatched"));
    }

    @Test
    void testTelegramNotificationServiceWithChatId() {
        TelegramNotificationService telegramService = new TelegramNotificationService(emergencyContactRepository);
        ReflectionTestUtils.setField(telegramService, "defaultChatId", "-1001234567890");
        assertEquals("-1001234567890", telegramService.getDefaultChatId());
        assertTrue(telegramService.isAuthorizedChatConfigured());
    }

    @Test
    void testSosNotificationServiceUnconfiguredMail() {
        SosNotificationService mailService = new SosNotificationService(null);
        assertFalse(mailService.isMailConfigured());
        assertFalse(mailService.testEmailConfiguration("test@example.com"));

        Grievance dummyGrievance = new Grievance();
        dummyGrievance.setTrackingId("SS-2026-TEST");
        dummyGrievance.setCitizen(new User("Test Citizen", "citizen@example.com", "pass", User.Role.CITIZEN));

        assertFalse(mailService.sendGrievanceConfirmation(dummyGrievance));
        assertFalse(mailService.sendGrievanceStatusUpdate(dummyGrievance, GrievanceStatus.PENDING, GrievanceStatus.RESOLVED, "Fixed"));
    }

    @Test
    void testGrievanceCreationDispatchesNotificationsSafely() {
        User citizen = new User("Pooja Sharma", "pooja@smartshield.gov.in", "pass123", User.Role.CITIZEN);
        citizen.setEmail("pooja@smartshield.gov.in");
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));

        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> {
            Grievance g = invocation.getArgument(0);
            ReflectionTestUtils.setField(g, "id", 101L);
            return g;
        });

        CreateGrievanceRequest req = new CreateGrievanceRequest();
        req.setCategory(GrievanceCategory.ROADS);
        req.setPriority(GrievancePriority.HIGH);
        req.setTitle("Severe Pothole on Highway");
        req.setDescription("Multiple deep potholes causing traffic slowdown");
        req.setLocation("NH-24, Ghaziabad");

        GrievanceResponse res = grievanceService.createGrievance(1L, req);

        assertNotNull(res);
        assertNotNull(res.getTrackingId());
        assertEquals(GrievanceStatus.PENDING, res.getStatus());

        // Verify that notification dispatches were triggered
        assertTrue(testSosNotificationService.confirmationCalled);
        assertTrue(testTelegramNotificationService.recvCalled);
    }

    @Test
    void testGrievanceStatusUpdateDispatchesNotificationsSafely() {
        User citizen = new User("Pooja Sharma", "pooja@smartshield.gov.in", "pass123", User.Role.CITIZEN);
        Grievance existing = new Grievance(
                "SS-2026-0001",
                citizen,
                GrievanceCategory.ROADS,
                GrievancePriority.HIGH,
                "Severe Pothole",
                "Pothole on Highway",
                "NH-24",
                "CITIZEN"
        );
        existing.setStatus(GrievanceStatus.PENDING);

        when(grievanceRepository.findByTrackingId("SS-2026-0001")).thenReturn(Optional.of(existing));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GrievanceResponse res = grievanceService.updateGrievanceStatus(
                "SS-2026-0001",
                GrievanceStatus.IN_REVIEW,
                "ADMIN001",
                "Maintenance team deployed"
        );

        assertNotNull(res);
        assertEquals(GrievanceStatus.IN_REVIEW, res.getStatus());

        assertTrue(testSosNotificationService.statusUpdateCalled);
        assertTrue(testTelegramNotificationService.updateCalled);
    }
}
