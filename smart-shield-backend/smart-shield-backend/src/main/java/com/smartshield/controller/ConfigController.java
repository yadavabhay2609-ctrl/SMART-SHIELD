package com.smartshield.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshield.dto.PublicConfigResponse;

@RestController
@RequestMapping("/api")
public class ConfigController {

    @Value("${maps.api-key:}")
    private String mapsApiKey;

    @GetMapping("/config")
    public ResponseEntity<PublicConfigResponse> getConfig() {
        boolean isConfigured = mapsApiKey != null && !mapsApiKey.trim().isEmpty();
        String resolvedKey = isConfigured ? mapsApiKey.trim() : "";
        return ResponseEntity.ok(new PublicConfigResponse(resolvedKey, isConfigured));
    }
}
