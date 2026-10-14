package com.smartshield.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegalKnowledgeBaseServiceTest {

    private LegalKnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        service = new LegalKnowledgeBaseService();
    }

    @Test
    void testBlankQueryReturnsGreeting() {
        LegalKnowledgeBaseService.KnowledgeMatch match = service.findRelevantAnswer("");
        assertNotNull(match);
        assertFalse(match.isEmergency());
        assertTrue(match.getAnswer().contains("Smart Shield Civic & Legal Assistant"));
    }

    @Test
    void testZeroFirQuery() {
        LegalKnowledgeBaseService.KnowledgeMatch match = service.findRelevantAnswer("What is Zero FIR and where can I file it?");
        assertNotNull(match);
        assertTrue(match.getAnswer().contains("Zero FIR"));
        assertTrue(match.getAnswer().contains("jurisdiction"));
    }

    @Test
    void testRtiActQuery() {
        LegalKnowledgeBaseService.KnowledgeMatch match = service.findRelevantAnswer("How do I file an RTI application under RTI Act 2005?");
        assertNotNull(match);
        assertTrue(match.getAnswer().contains("Right to Information"));
        assertTrue(match.getAnswer().contains("30 days"));
    }

    @Test
    void testWomenSafetyGuideQuery() {
        LegalKnowledgeBaseService.KnowledgeMatch match = service.findRelevantAnswer("How does the women safety center and SOS work?");
        assertNotNull(match);
        assertTrue(match.getAnswer().contains("Women Safety"));
        assertTrue(match.getAnswer().contains("SOS"));
    }

    @Test
    void testEmergencyHelplineQuery() {
        LegalKnowledgeBaseService.KnowledgeMatch match = service.findRelevantAnswer("Emergency help! I am in danger");
        assertNotNull(match);
        assertTrue(match.isEmergency());
        assertTrue(match.getAnswer().contains("112"));
        assertTrue(match.getAnswer().contains("1091"));
    }
}
