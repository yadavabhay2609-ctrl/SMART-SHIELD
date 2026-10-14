package com.smartshield.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartshield.dto.FormScanResponse;
import com.smartshield.dto.FormScannerStatusResponse;
import com.smartshield.service.FormScannerService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller exposing Government Form Scanner and Document Understanding endpoints.
 * Includes public status inspection and authenticated citizen document scanning.
 */
@RestController
@RequestMapping("/api/forms")
public class FormScannerController {

    private static final Logger log = LoggerFactory.getLogger(FormScannerController.class);

    private final FormScannerService formScannerService;

    public FormScannerController(FormScannerService formScannerService) {
        this.formScannerService = formScannerService;
    }

    /**
     * Public endpoint reporting operational status, configured model, and supported formats.
     */
    @GetMapping("/status")
    public ResponseEntity<FormScannerStatusResponse> getStatus() {
        return ResponseEntity.ok(formScannerService.getStatus());
    }

    private Long resolveCitizenId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute("authenticatedCitizenId");
        if (attr instanceof Long l) {
            return l;
        }
        if (attr instanceof com.smartshield.model.User u) {
            return u.getId();
        }
        return null;
    }

    /**
     * Authenticated citizen endpoint to upload and temporarily scan a government document/form.
     */
    @PostMapping(value = "/scan")
    public ResponseEntity<FormScanResponse> scanForm(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "instruction", required = false) String instruction,
            HttpServletRequest request) {

        Long citizenId = resolveCitizenId(request);
        log.info("Government form scan requested by citizenId: {}", citizenId);

        FormScanResponse response = formScannerService.scanForm(file, instruction, citizenId);
        return ResponseEntity.ok(response);
    }
}
