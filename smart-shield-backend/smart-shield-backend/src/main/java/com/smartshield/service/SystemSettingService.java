package com.smartshield.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartshield.model.SystemSetting;
import com.smartshield.repository.SystemSettingRepository;

@Service
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    public Optional<SystemSetting> getSetting(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return systemSettingRepository.findBySettingKey(key);
    }

    public Optional<String> getSettingValue(String key) {
        return getSetting(key).map(SystemSetting::getSettingValue);
    }

    public String getSettingValue(String key, String defaultValue) {
        return getSettingValue(key).orElse(defaultValue);
    }

    @Transactional
    public SystemSetting setSetting(String key, String value, String description) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Setting key cannot be null or blank");
        }

        return systemSettingRepository.findBySettingKey(key)
                .map(existing -> {
                    existing.setSettingValue(value != null ? value : "");
                    if (description != null) {
                        existing.setDescription(description);
                    }
                    return systemSettingRepository.save(existing);
                })
                .orElseGet(() -> {
                    SystemSetting newSetting = new SystemSetting(key, value != null ? value : "", description);
                    return systemSettingRepository.save(newSetting);
                });
    }

    public boolean hasSetting(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return systemSettingRepository.existsBySettingKey(key);
    }
}
