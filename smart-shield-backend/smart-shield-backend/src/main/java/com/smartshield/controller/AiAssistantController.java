package com.smartshield.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.AiChatRequest;
import com.smartshield.dto.AiChatResponse;
import com.smartshield.dto.AiStatusResponse;
import com.smartshield.model.User;
import com.smartshield.service.AiAssistantService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for Smart Shield Civic & Legal AI Assistant.
 * Chat endpoint guarded by CitizenAuthInterceptor; status endpoint publicly inspectable.
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
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
     * 1. GET /api/ai/status
     * Public endpoint reporting operational status of AI Assistant & local fallback knowledge base.
     */
    @GetMapping("/status")
    public ResponseEntity<AiStatusResponse> getStatus() {
        AiStatusResponse status = aiAssistantService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 2. POST /api/ai/chat
     * Authenticated citizen conversational guidance endpoint.
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            HttpServletRequest httpRequest) {

        Long citizenId = resolveCitizenId(httpRequest);
        if (citizenId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Citizen authentication required");
        }

        String clientIp = resolveClientIp(httpRequest);
        AiChatResponse response = aiAssistantService.processQuery(citizenId, request, clientIp);
        return ResponseEntity.ok(response);
    }
}
