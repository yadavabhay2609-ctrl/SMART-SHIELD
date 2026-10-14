package com.smartshield.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshield.dto.AdminSettingsResponse;
import com.smartshield.dto.AdminSettingsUpdateRequest;
import com.smartshield.service.SystemSettingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final SystemSettingService systemSettingService;

    public static final String KEY_AUTO_FILE_ENABLED = "auto_file_enabled";
    public static final String KEY_AUTO_FILE_THRESHOLD = "auto_file_threshold";
    public static final String KEY_MIN_RATINGS_FOR_AUTO_FILE = "min_ratings_for_auto_file";

    public AdminSettingsController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping
    public ResponseEntity<AdminSettingsResponse> getSettings() {
        boolean autoFileEnabled = Boolean.parseBoolean(
                systemSettingService.getSettingValue(KEY_AUTO_FILE_ENABLED, "false"));
        int autoFileThreshold = Integer.parseInt(
                systemSettingService.getSettingValue(KEY_AUTO_FILE_THRESHOLD, "3"));
        int minRatingsForAutoFile = Integer.parseInt(
                systemSettingService.getSettingValue(KEY_MIN_RATINGS_FOR_AUTO_FILE, "5"));

        return ResponseEntity.ok(new AdminSettingsResponse(
                autoFileEnabled,
                autoFileThreshold,
                minRatingsForAutoFile
        ));
    }

    @PutMapping
    public ResponseEntity<AdminSettingsResponse> updateSettings(
            @Valid @RequestBody AdminSettingsUpdateRequest request) {

        systemSettingService.setSetting(
                KEY_AUTO_FILE_ENABLED,
                String.valueOf(request.getAuto_file_enabled()),
                "Whether automated filing is enabled when threshold is reached"
        );
        systemSettingService.setSetting(
                KEY_AUTO_FILE_THRESHOLD,
                String.valueOf(request.getAuto_file_threshold()),
                "Threshold rating required for auto filing"
        );
        systemSettingService.setSetting(
                KEY_MIN_RATINGS_FOR_AUTO_FILE,
                String.valueOf(request.getMin_ratings_for_auto_file()),
                "Minimum number of ratings required before auto filing triggers"
        );

        return getSettings();
    }
}
