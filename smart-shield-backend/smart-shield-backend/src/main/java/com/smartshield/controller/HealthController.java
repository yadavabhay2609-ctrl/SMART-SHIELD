package com.smartshield.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirf setup verify karne ke liye: server chal raha hai aur MySQL connect hai ya nahi.
 * Test: http://localhost:8080/api/health
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("app", "Smart Shield Backend");
        res.put("status", "UP");
        res.put("time", LocalDateTime.now().toString());

        try (var conn = dataSource.getConnection()) {
            res.put("database", conn.isValid(2) ? "CONNECTED" : "NOT_CONNECTED");
        } catch (Exception e) {
            res.put("database", "ERROR: " + e.getMessage());
        }
        return res;
    }
}
