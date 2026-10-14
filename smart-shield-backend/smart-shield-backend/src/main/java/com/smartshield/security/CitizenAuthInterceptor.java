package com.smartshield.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshield.dto.ErrorResponse;
import com.smartshield.model.User;
import com.smartshield.repository.UserRepository;
import com.smartshield.service.CitizenTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that guards protected citizen endpoints by validating HMAC-SHA256 citizen tokens.
 * Verifies signature, expiration, CITIZEN role, and database existence.
 * Attaches authenticated citizen attributes to the request context.
 */
@Component
public class CitizenAuthInterceptor implements HandlerInterceptor {

    private final CitizenTokenService citizenTokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CitizenAuthInterceptor(CitizenTokenService citizenTokenService,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper) {
        this.citizenTokenService = citizenTokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS pre-flight OPTIONS requests through
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(request, response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorizedResponse(request, response, "Authentication token cannot be empty");
            return false;
        }

        // 1. Validate signature and expiration
        if (!citizenTokenService.validateToken(token)) {
            sendUnauthorizedResponse(request, response, "Invalid or expired citizen token");
            return false;
        }

        // 2. Validate role is CITIZEN
        String role = citizenTokenService.extractRole(token);
        if (!"CITIZEN".equals(role)) {
            sendForbiddenResponse(request, response, "Insufficient privileges. Citizen token required.");
            return false;
        }

        // 3. Extract userId
        Long userId = citizenTokenService.extractUserId(token);
        if (userId == null) {
            sendUnauthorizedResponse(request, response, "Invalid token claims: missing citizen user ID");
            return false;
        }

        // 4. Verify citizen user exists in UserRepository
        User citizen = userRepository.findById(userId).orElse(null);
        if (citizen == null) {
            sendUnauthorizedResponse(request, response, "Citizen user not found");
            return false;
        }

        // 5. Verify role matches CITIZEN in database
        if (citizen.getRole() != User.Role.CITIZEN) {
            sendForbiddenResponse(request, response, "Account is not a citizen account");
            return false;
        }

        // Attach trusted citizen identity to request attributes for downstream controller/service use
        request.setAttribute("authenticatedCitizenId", citizen.getId());
        request.setAttribute("authenticatedCitizen", citizen);

        return true;
    }

    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        sendErrorResponse(request, response, HttpStatus.UNAUTHORIZED, message);
    }

    private void sendForbiddenResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        sendErrorResponse(request, response, HttpStatus.FORBIDDEN, message);
    }

    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
