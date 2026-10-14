package com.smartshield.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshield.dto.ErrorResponse;
import com.smartshield.model.AdminUser;
import com.smartshield.repository.AdminUserRepository;
import com.smartshield.service.AdminAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthService adminAuthService;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminAuthService adminAuthService,
                                AdminUserRepository adminUserRepository,
                                ObjectMapper objectMapper) {
        this.adminAuthService = adminAuthService;
        this.adminUserRepository = adminUserRepository;
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
            sendUnauthorizedResponse(request, response, "Token cannot be empty");
            return false;
        }

        // 1. Validate signature and expiration
        if (!adminAuthService.validateToken(token)) {
            sendUnauthorizedResponse(request, response, "Invalid or expired admin token");
            return false;
        }

        // 2. Validate role is ADMIN
        String role = adminAuthService.extractRole(token);
        if (!"ADMIN".equals(role)) {
            sendUnauthorizedResponse(request, response, "Insufficient privileges");
            return false;
        }

        // 3. Extract adminId
        String adminId = adminAuthService.extractAdminId(token);
        if (adminId == null || adminId.isBlank()) {
            sendUnauthorizedResponse(request, response, "Invalid token payload");
            return false;
        }

        // 4. Verify admin user exists in AdminUserRepository
        AdminUser admin = adminUserRepository.findByAdminId(adminId).orElse(null);
        if (admin == null) {
            sendUnauthorizedResponse(request, response, "Admin user not found");
            return false;
        }

        // 5. Verify admin user is active
        if (!admin.isActive()) {
            sendUnauthorizedResponse(request, response, "Admin account is deactivated");
            return false;
        }

        // Store authenticated admin in request attributes for downstream use
        request.setAttribute("authenticatedAdmin", admin);
        request.setAttribute("adminId", adminId);

        return true;
    }

    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
