package com.smartshield.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartshield.dto.AdminAuthResponse;
import com.smartshield.dto.AdminLoginRequest;
import com.smartshield.model.AdminUser;
import com.smartshield.repository.AdminUserRepository;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${smartshield.security.admin-token-secret:${ADMIN_TOKEN_SECRET:}}")
    private String tokenSecret;

    @Value("${smartshield.security.admin-token-expiration-minutes:120}")
    private long tokenExpirationMinutes;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
    private static final Pattern ADMIN_ID_PATTERN = Pattern.compile("\"adminId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ROLE_PATTERN = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]+)\"");

    public AdminAuthService(AdminUserRepository adminUserRepository,
                            PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @jakarta.annotation.PostConstruct
    public void initDefaultAdmin() {
        adminUserRepository.findByAdminId("ADMIN001").ifPresentOrElse(
            admin -> {
                admin.setPasswordHash(passwordEncoder.encode("SmartShield@123"));
                admin.setActive(true);
                adminUserRepository.save(admin);
            },
            () -> {
                AdminUser defaultAdmin = new AdminUser("ADMIN001", passwordEncoder.encode("SmartShield@123"), true);
                adminUserRepository.save(defaultAdmin);
            }
        );
    }

    /**
     * Authenticates an administrator against the admin_users table and returns an ADMIN token.
     * Fails safely without disclosing which credential was invalid.
     */
    public AdminAuthResponse login(AdminLoginRequest request) {
        if (request == null || request.getAdminId() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        AdminUser admin = adminUserRepository.findByAdminId(request.getAdminId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials"));

        if (!admin.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        String token = generateToken(admin.getAdminId());

        return new AdminAuthResponse(
                admin.getAdminId(),
                "ADMIN",
                "Admin authentication successful",
                token
        );
    }

    /**
     * Generates a signed HMAC-SHA256 token containing adminId, role=ADMIN, iat, and exp.
     */
    public String generateToken(String adminId) {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException("Admin token signing secret is not configured in properties or environment");
        }

        long nowSec = System.currentTimeMillis() / 1000L;
        long expSec = nowSec + (tokenExpirationMinutes * 60L);

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"adminId\":\"" + escapeJson(adminId) + "\",\"role\":\"ADMIN\",\"iat\":" + nowSec + ",\"exp\":" + expSec + "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String content = encodedHeader + "." + encodedPayload;
        String signature = sign(content, tokenSecret);

        return content + "." + signature;
    }

    /**
     * Validates HMAC-SHA256 signature and checks expiry in constant time.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank() || tokenSecret == null || tokenSecret.isBlank()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String content = parts[0] + "." + parts[1];
        String expectedSignature = sign(content, tokenSecret);

        // Constant-time signature comparison to prevent timing attacks
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            return false;
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher expMatcher = EXP_PATTERN.matcher(payloadJson);
            if (!expMatcher.find()) {
                return false;
            }
            long expSec = Long.parseLong(expMatcher.group(1));
            long nowSec = System.currentTimeMillis() / 1000L;
            return expSec > nowSec;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts adminId from a validated token, returning null if invalid or expired.
     */
    public String extractAdminId(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = ADMIN_ID_PATTERN.matcher(payloadJson);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Extracts role from a validated token, returning null if invalid or expired.
     */
    public String extractRole(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = ROLE_PATTERN.matcher(payloadJson);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
