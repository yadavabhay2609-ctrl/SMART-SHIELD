package com.smartshield.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating and validating server-signed HMAC-SHA256 tokens for citizen authentication.
 * Guarantees cryptographic integrity, constant-time signature validation, and claim verification.
 */
@Service
public class CitizenTokenService {

    @Value("${smartshield.security.citizen-token-secret:${CITIZEN_TOKEN_SECRET:smartshield-local-citizen-token-secret-key-32chars}}")
    private String tokenSecret;

    @Value("${smartshield.security.citizen-token-expiration-minutes:120}")
    private long tokenExpirationMinutes;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("\"userId\"\\s*:\\s*(\\d+)");
    private static final Pattern ROLE_PATTERN = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"");

    public CitizenTokenService() {
    }

    public CitizenTokenService(String tokenSecret, long tokenExpirationMinutes) {
        this.tokenSecret = tokenSecret;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    /**
     * Generates a signed HMAC-SHA256 citizen token containing userId, role=CITIZEN, email, iat, and exp.
     *
     * @param userId citizen user ID
     * @param email citizen email
     * @return 3-part signed token string
     */
    public String generateToken(Long userId, String email) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null when generating citizen token");
        }
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException("Citizen token signing secret is not configured");
        }

        long nowSec = System.currentTimeMillis() / 1000L;
        long expSec = nowSec + (tokenExpirationMinutes * 60L);

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"userId\":" + userId + ",\"role\":\"CITIZEN\",\"email\":\"" + escapeJson(email) + "\",\"iat\":" + nowSec + ",\"exp\":" + expSec + "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String content = encodedHeader + "." + encodedPayload;
        String signature = sign(content, tokenSecret);

        return content + "." + signature;
    }

    /**
     * Validates HMAC-SHA256 signature and checks expiration in constant time.
     *
     * @param token the bearer token string
     * @return true if token is valid and unexpired, false otherwise
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
     * Extracts userId from a validated token, returning null if invalid or expired.
     */
    public Long extractUserId(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = USER_ID_PATTERN.matcher(payloadJson);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
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

    /**
     * Extracts email from a validated token, returning null if invalid or expired.
     */
    public String extractEmail(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = EMAIL_PATTERN.matcher(payloadJson);
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
