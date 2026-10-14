package com.smartshield.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshield.dto.ApiResponse;
import com.smartshield.service.TelegramNotificationService;

/**
 * Public webhook endpoint for Telegram Bot updates.
 * Can be called by Telegram Webhook API or used for automated integration testing.
 */
@RestController
@RequestMapping("/api/public/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramNotificationService telegramNotificationService;

    public TelegramWebhookController(TelegramNotificationService telegramNotificationService) {
        this.telegramNotificationService = telegramNotificationService;
    }

    /**
     * Receives standard Telegram Webhook JSON updates.
     * Example payload: {"message": {"chat": {"id": 123456}, "text": "/start SH-AB12CD"}}
     */
    @PostMapping("/webhook")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Void>> handleTelegramWebhook(@RequestBody(required = false) Map<String, Object> update) {
        if (update == null) {
            return ResponseEntity.ok(ApiResponse.success("Ignored empty update"));
        }

        try {
            Object msgObj = update.get("message");
            if (msgObj instanceof Map<?, ?> msg) {
                Object textObj = msg.get("text");
                Object chatObj = msg.get("chat");
                if (textObj instanceof String text && chatObj instanceof Map<?, ?> chat) {
                    Object chatIdObj = chat.get("id");
                    if (chatIdObj != null) {
                        String chatId = String.valueOf(chatIdObj);
                        String cleanText = text.trim();
                        // Extract SH-XXXXXX if present
                        if (cleanText.contains("SH-")) {
                            int idx = cleanText.indexOf("SH-");
                            String codeCandidate = cleanText.substring(idx).split("\\s+")[0].trim();
                            boolean linked = telegramNotificationService.verifyAndLinkCode(codeCandidate, chatId);
                            if (linked) {
                                log.info("Successfully linked Telegram contact via webhook for code {}", codeCandidate);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Webhook update processing exception: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Webhook processed"));
    }

    /**
     * Helper endpoint to submit a connection code and chat ID directly.
     */
    @PostMapping("/connect-code")
    public ResponseEntity<ApiResponse<Boolean>> submitConnectionCode(@RequestBody Map<String, String> payload) {
        String code = payload != null ? payload.get("connectionCode") : null;
        String chatId = payload != null ? payload.get("chatId") : null;

        if (code == null || code.isBlank() || chatId == null || chatId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("connectionCode and chatId are required"));
        }

        boolean success = telegramNotificationService.verifyAndLinkCode(code, chatId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Telegram chat linked successfully", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired connection code"));
        }
    }
}
