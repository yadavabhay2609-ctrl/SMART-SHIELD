package com.smartshield.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.AdminAuthResponse;
import com.smartshield.dto.AdminLoginRequest;
import com.smartshield.model.AdminUser;
import com.smartshield.repository.AdminUserRepository;

class AdminAuthServiceTest {

    private AdminUserRepository adminUserRepository;
    private PasswordEncoder passwordEncoder;
    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminUserRepository = Mockito.mock(AdminUserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        adminAuthService = new AdminAuthService(adminUserRepository, passwordEncoder);
        ReflectionTestUtils.setField(adminAuthService, "tokenSecret", "smartshield-test-admin-secret-key-32chars");
        ReflectionTestUtils.setField(adminAuthService, "tokenExpirationMinutes", 120L);
    }

    @Test
    void testInitDefaultAdmin_WhenNew() {
        when(adminUserRepository.findByAdminId("ADMIN001")).thenReturn(Optional.empty());

        adminAuthService.initDefaultAdmin();

        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    void testInitDefaultAdmin_WhenExisting() {
        AdminUser existing = new AdminUser("ADMIN001", "old-hash", true);
        when(adminUserRepository.findByAdminId("ADMIN001")).thenReturn(Optional.of(existing));

        adminAuthService.initDefaultAdmin();

        verify(adminUserRepository).save(existing);
        assertTrue(passwordEncoder.matches("SmartShield@123", existing.getPasswordHash()));
        assertTrue(existing.isActive());
    }

    @Test
    void testLogin_Success() {
        String hash = passwordEncoder.encode("SmartShield@123");
        AdminUser admin = new AdminUser("ADMIN001", hash, true);
        when(adminUserRepository.findByAdminId("ADMIN001")).thenReturn(Optional.of(admin));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setAdminId("ADMIN001");
        request.setPassword("SmartShield@123");

        AdminAuthResponse response = adminAuthService.login(request);

        assertNotNull(response);
        assertEquals("ADMIN001", response.getAdminId());
        assertEquals("ADMIN", response.getRole());
        assertNotNull(response.getToken());

        assertTrue(adminAuthService.validateToken(response.getToken()));
        assertEquals("ADMIN001", adminAuthService.extractAdminId(response.getToken()));
        assertEquals("ADMIN", adminAuthService.extractRole(response.getToken()));
    }

    @Test
    void testLogin_WrongPassword_ThrowsUnauthorized() {
        String hash = passwordEncoder.encode("SmartShield@123");
        AdminUser admin = new AdminUser("ADMIN001", hash, true);
        when(adminUserRepository.findByAdminId("ADMIN001")).thenReturn(Optional.of(admin));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setAdminId("ADMIN001");
        request.setPassword("WrongPassword@999");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminAuthService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testLogin_WrongAdminId_ThrowsUnauthorized() {
        when(adminUserRepository.findByAdminId("WRONG_ID")).thenReturn(Optional.empty());

        AdminLoginRequest request = new AdminLoginRequest();
        request.setAdminId("WRONG_ID");
        request.setPassword("SmartShield@123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminAuthService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testLogin_InactiveAdmin_ThrowsUnauthorized() {
        String hash = passwordEncoder.encode("SmartShield@123");
        AdminUser admin = new AdminUser("ADMIN001", hash, false);
        when(adminUserRepository.findByAdminId("ADMIN001")).thenReturn(Optional.of(admin));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setAdminId("ADMIN001");
        request.setPassword("SmartShield@123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminAuthService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testValidateToken_TamperedToken_ReturnsFalse() {
        String token = adminAuthService.generateToken("ADMIN001");
        String tamperedToken = token.substring(0, token.length() - 5) + "ABCDE";
        assertFalse(adminAuthService.validateToken(tamperedToken));
    }
}
