package com.smartshield.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.AiChatRequest;
import com.smartshield.dto.AiChatResponse;
import com.smartshield.dto.AiStatusResponse;
import com.smartshield.util.RateLimiterUtil;

/**
 * High-level orchestration service for the Smart Shield AI Assistant.
 * Coordinates rate limiting, prompt injection defense, emergency safety overrides,
 * Gemini API queries, and deterministic local civic/legal knowledge base fallbacks.
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final GeminiService geminiService;
    private final LegalKnowledgeBaseService knowledgeBaseService;
    private final RateLimiterUtil rateLimiterUtil;

    // Rate Limit: 10 queries per citizen per minute
    private static final int MAX_QUERIES_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    public AiAssistantService(GeminiService geminiService,
                              LegalKnowledgeBaseService knowledgeBaseService,
                              RateLimiterUtil rateLimiterUtil) {
        this.geminiService = geminiService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.rateLimiterUtil = rateLimiterUtil;
    }

    /**
     * Processes a citizen AI query with rate limiting, safety filtering, and transparent fallback.
     */
    public AiChatResponse processQuery(Long citizenId, AiChatRequest request, String clientIp) {
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        String rawMessage = request.getMessage().trim();
        if (rawMessage.length() > 3000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum allowed length of 3000 characters");
        }

        // 1. Rate Limiting per citizen
        String rateKey = "AI_CITIZEN_" + citizenId;
        if (!rateLimiterUtil.tryAcquire(rateKey, MAX_QUERIES_PER_MINUTE, RATE_LIMIT_WINDOW_MS)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests to the AI Assistant. Please wait a moment before sending another query."
            );
        }

        // 2. Prompt Injection & Secret Extraction Defense
        String normalizedQuery = rawMessage.toLowerCase(Locale.ROOT);
        if (containsPromptInjection(normalizedQuery)) {
            return new AiChatResponse(
                    "I am the Smart Shield Civic & Legal Assistant. My instructions are designed to help citizens with public complaints, city services, and general legal information. I cannot reveal internal keys, credentials, or override system boundaries. How can I assist you with your civic questions today?",
                    "LOCAL_KB",
                    true,
                    List.of("File a Grievance", "Safe Routes Guide", "Emergency Helplines")
            );
        }

        // 3. Emergency Safety Check
        LegalKnowledgeBaseService.KnowledgeMatch kbMatch = knowledgeBaseService.findRelevantAnswer(rawMessage);
        if (kbMatch.isEmergency()) {
            return AiChatResponse.ofEmergency(kbMatch.getAnswer());
        }

        // 4. Query Google Gemini if configured
        if (geminiService.isConfigured()) {
            Optional<String> geminiAnswerOpt = geminiService.generateAnswer(rawMessage, request.getHistory());
            if (geminiAnswerOpt.isPresent()) {
                List<String> actions = determineSuggestedActions(rawMessage);
                return AiChatResponse.ofGemini(geminiAnswerOpt.get(), actions);
            } else {
                log.warn("Gemini call returned empty or failed. Engaging verified local knowledge base fallback.");
            }
        }

        // 5. Fallback: Local Civic & Legal Knowledge Base
        return AiChatResponse.ofLocalKb(kbMatch.getAnswer(), kbMatch.getSuggestedActions());
    }

    /**
     * Returns public configuration status of AI services.
     */
    public AiStatusResponse getStatus() {
        boolean configured = geminiService.isConfigured();
        String model = geminiService.getModelName();
        boolean localKb = true;

        String msg;
        if (configured) {
            msg = "Gemini AI model (" + model + ") operational with verified Smart Shield civic knowledge base.";
        } else {
            msg = "Gemini API key unconfigured. Smart Shield verified local civic & legal knowledge base is active.";
        }

        return new AiStatusResponse(true, configured, model, localKb, msg);
    }

    private boolean containsPromptInjection(String q) {
        return q.contains("ignore previous instructions")
                || q.contains("reveal your system prompt")
                || q.contains("show system prompt")
                || q.contains("show api key")
                || q.contains("print api key")
                || q.contains("database password")
                || q.contains("reveal secret")
                || q.contains("act as developer")
                || q.contains("act as root")
                || q.contains("bypass rules");
    }

    private List<String> determineSuggestedActions(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<String> actions = new ArrayList<>();
        if (q.contains("road") || q.contains("light") || q.contains("garbage") || q.contains("water") || q.contains("complain") || q.contains("grievance")) {
            actions.add("Raise Complaint in Grievances");
            actions.add("View My Grievances");
        }
        if (q.contains("route") || q.contains("walk") || q.contains("travel") || q.contains("map")) {
            actions.add("Open Safe Routes");
            actions.add("Calculate Route Safety");
        }
        if (q.contains("sos") || q.contains("women") || q.contains("help") || q.contains("contact")) {
            actions.add("Women Safety & SOS Center");
            actions.add("Helpline Directory");
        }
        if (actions.isEmpty()) {
            actions.add("Raise Complaint");
            actions.add("Safe Routes");
            actions.add("Women Safety");
        }
        return actions;
    }
}
