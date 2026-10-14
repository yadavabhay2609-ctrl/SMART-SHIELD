package com.smartshield.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshield.dto.FormScanFieldDto;
import com.smartshield.dto.FormScanResponse;
import com.smartshield.dto.FormScannerStatusResponse;
import com.smartshield.util.FileValidationUtil;
import com.smartshield.util.RateLimiterUtil;

/**
 * Service orchestrating secure government form scanning, strict validation,
 * zero permanent storage, multimodal Gemini integration, and sensitive data masking.
 */
@Service
public class FormScannerService {

    private static final Logger log = LoggerFactory.getLogger(FormScannerService.class);

    private final GeminiService geminiService;
    private final FileValidationUtil fileValidationUtil;
    private final RateLimiterUtil rateLimiterUtil;
    private final ObjectMapper objectMapper;

    // Rate limiting: 5 form scans per minute per citizen
    private static final int MAX_SCANS_PER_MINUTE = 5;
    private static final long SCAN_WINDOW_MILLIS = 60_000L;

    // Regex patterns for sensitive data masking
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?(\\d{4})\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}(\\d{4})[A-Z]\\b");
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("\\b\\d{5,14}(\\d{4})\\b");

    public FormScannerService(GeminiService geminiService,
                              FileValidationUtil fileValidationUtil,
                              RateLimiterUtil rateLimiterUtil,
                              ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.fileValidationUtil = fileValidationUtil;
        this.rateLimiterUtil = rateLimiterUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Reports public status and configuration of the Government Form Scanner.
     */
    public FormScannerStatusResponse getStatus() {
        boolean configured = geminiService.isConfigured();
        String model = geminiService.getModelName();
        List<String> supportedMimes = List.of("application/pdf", "image/jpeg", "image/png", "image/webp");
        long maxBytes = FileValidationUtil.DEFAULT_MAX_FILE_SIZE_BYTES;

        String msg;
        if (configured) {
            msg = "Government Form Scanner active with Gemini multimodal AI (" + model + "). Zero permanent storage enabled.";
        } else {
            msg = "Gemini API key is unconfigured. Form scanning requires configured Gemini credentials.";
        }

        return new FormScannerStatusResponse(true, configured, model, supportedMimes, maxBytes, msg);
    }

    /**
     * Validates and temporarily scans a government form or document without permanent storage.
     *
     * @param file uploaded form document or image
     * @param instruction optional citizen query/check
     * @param citizenId authenticated citizen ID
     * @return structured FormScanResponse
     */
    public FormScanResponse scanForm(MultipartFile file, String instruction, Long citizenId) {
        // 1. Strict File Presence Check
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document or file was uploaded for scanning.");
        }

        // 2. Rate Limiting: 5 scans per minute per citizen
        String limiterKey = "form_scan:" + (citizenId != null ? citizenId : "anonymous");
        if (!rateLimiterUtil.tryAcquire(limiterKey, MAX_SCANS_PER_MINUTE, SCAN_WINDOW_MILLIS)) {
            log.warn("Form scan rate limit exceeded for citizenId: {}", citizenId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Form scanning limit reached (5 scans per minute per citizen). Please wait before scanning another document.");
        }

        // 3. File Validation (10MB limit, extension, path traversal, and magic bytes for PDF, JPG, PNG, WEBP)
        String detectedMime;
        try {
            detectedMime = fileValidationUtil.validateEvidenceFile(file);
        } catch (IllegalArgumentException e) {
            log.warn("Form document validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        // 4. Temporary in-memory processing lifecycle - NO permanent storage
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded form document bytes: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded document.");
        }

        try {
            // 5. Honest Not-Configured Check
            if (!geminiService.isConfigured()) {
                log.info("Form scan requested while Gemini is unconfigured. Returning honest NOT_CONFIGURED state.");
                return FormScanResponse.notConfigured(
                        "Government Form Scanner requires Gemini document processing configuration (GEMINI_API_KEY). " +
                        "Real document understanding cannot be fabricated.");
            }

            // 6. Invoke Gemini Multimodal API with Base64 inline_data
            Optional<String> geminiResultOpt = geminiService.analyzeDocument(fileBytes, detectedMime, instruction);
            if (geminiResultOpt.isEmpty()) {
                return FormScanResponse.failure("UNAVAILABLE",
                        "Gemini document analysis service was temporarily unable to process this document. Please ensure the document is clear and readable.");
            }

            // 7. Parse and Validate Structured JSON Result
            return parseAndSanitizeGeminiOutput(geminiResultOpt.get());

        } finally {
            // Explicitly dereference temporary byte array for garbage collection
            fileBytes = null;
        }
    }

    /**
     * Parses the JSON output from Gemini, validates against DTO constraints,
     * sanitizes text, and masks sensitive personal identifiers.
     */
    private FormScanResponse parseAndSanitizeGeminiOutput(String rawJson) {
        try {
            // Strip any accidental markdown formatting if present
            String cleanedJson = rawJson.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            JsonNode root = objectMapper.readTree(cleanedJson);

            FormScanResponse response = new FormScanResponse();
            response.setSuccess(true);
            response.setSource("GEMINI");
            response.setDocumentStored(false);

            // Document type & confidence
            String docType = root.path("documentType").asText("UNKNOWN");
            response.setDocumentType(sanitizeText(docType, 120));

            int docConfidence = root.path("documentTypeConfidence").asInt(80);
            response.setDocumentTypeConfidence(clampConfidence(docConfidence));

            // Summary
            String summary = root.path("summary").asText("Government document analyzed successfully.");
            response.setSummary(sanitizeText(summary, 500));

            // Page count
            int pageCount = root.path("pageCount").asInt(1);
            response.setPageCount(Math.max(1, pageCount));

            // Fields list
            List<FormScanFieldDto> fields = new ArrayList<>();
            JsonNode fieldsNode = root.path("fields");
            if (fieldsNode.isArray()) {
                int count = 0;
                for (JsonNode fNode : fieldsNode) {
                    if (count++ >= 50) break; // Maximum 50 fields limit

                    String label = sanitizeText(fNode.path("label").asText("Field"), 100);
                    String name = sanitizeText(fNode.path("name").asText("field_" + count), 100);
                    String rawValue = sanitizeText(fNode.path("value").asText(""), 250);
                    String maskedValue = maskSensitiveData(rawValue, label);

                    String rawStatus = fNode.path("status").asText("UNCLEAR").toUpperCase();
                    String status = normalizeFieldStatus(rawStatus);

                    int conf = clampConfidence(fNode.path("confidence").asInt(80));
                    String guidance = sanitizeText(fNode.path("guidance").asText(""), 300);
                    boolean verificationRequired = fNode.path("verificationRequired").asBoolean(false);

                    fields.add(new FormScanFieldDto(label, name, maskedValue, status, conf, guidance, verificationRequired));
                }
            }
            response.setFields(fields);

            // Missing fields list
            List<String> missingFields = new ArrayList<>();
            JsonNode missingNode = root.path("missingFields");
            if (missingNode.isArray()) {
                for (JsonNode m : missingNode) {
                    String mText = sanitizeText(m.asText(), 100);
                    if (!mText.isBlank()) {
                        missingFields.add(mText);
                    }
                }
            }
            response.setMissingFields(missingFields);

            // Warnings list
            List<String> warnings = new ArrayList<>();
            JsonNode warnNode = root.path("warnings");
            if (warnNode.isArray()) {
                for (JsonNode w : warnNode) {
                    String wText = sanitizeText(w.asText(), 200);
                    if (!wText.isBlank()) {
                        warnings.add(wText);
                    }
                }
            }
            response.setWarnings(warnings);

            // Verification items list
            List<String> verificationItems = new ArrayList<>();
            JsonNode verNode = root.path("verificationItems");
            if (verNode.isArray()) {
                for (JsonNode v : verNode) {
                    String vText = sanitizeText(v.asText(), 200);
                    if (!vText.isBlank()) {
                        verificationItems.add(vText);
                    }
                }
            }
            response.setVerificationItems(verificationItems);

            return response;

        } catch (Exception e) {
            log.error("Failed to parse Gemini structured JSON output: {}", e.getMessage());
            return FormScanResponse.failure("INVALID_AI_RESPONSE",
                    "AI form analysis output was not in expected structured format. Please re-scan with a clearer document.");
        }
    }

    private String normalizeFieldStatus(String status) {
        return switch (status) {
            case "FILLED" -> "FILLED";
            case "EMPTY" -> "EMPTY";
            case "NOT_APPLICABLE", "N/A" -> "NOT_APPLICABLE";
            default -> "UNCLEAR";
        };
    }

    private int clampConfidence(int conf) {
        if (conf < 0) return 0;
        if (conf > 100) return 100;
        return conf;
    }

    private String sanitizeText(String text, int maxLength) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    /**
     * Masks sensitive identifiers like Aadhaar, PAN, and Bank Account numbers.
     */
    private String maskSensitiveData(String val, String label) {
        if (val == null || val.isBlank()) return "";

        String lowerLabel = label != null ? label.toLowerCase() : "";
        if (lowerLabel.contains("password") || lowerLabel.contains("pin") || lowerLabel.contains("otp")) {
            return "[SENSITIVE VALUE DETECTED — VERIFY ON ORIGINAL DOCUMENT]";
        }

        // Mask Aadhaar numbers (XXXX-XXXX-1234)
        String masked = AADHAAR_PATTERN.matcher(val).replaceAll("XXXX-XXXX-$1");

        // Mask PAN numbers (XXXXX1234X)
        masked = PAN_PATTERN.matcher(masked).replaceAll("XXXXX$1X");

        // Mask Bank account numbers (********1234)
        masked = BANK_ACCOUNT_PATTERN.matcher(masked).replaceAll("********$1");

        return masked;
    }
}
