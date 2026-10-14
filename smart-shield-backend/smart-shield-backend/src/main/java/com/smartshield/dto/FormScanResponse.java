package com.smartshield.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard structured response for government form and document scans.
 */
public class FormScanResponse {

    private boolean success;
    private String source; // "GEMINI", "NOT_CONFIGURED", "UNAVAILABLE", "INVALID_DOCUMENT"
    private String documentType;
    private Integer documentTypeConfidence;
    private String summary;
    private Integer pageCount;
    private List<FormScanFieldDto> fields = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> verificationItems = new ArrayList<>();
    private String disclaimer = "This scan provides AI-assisted document guidance only. Verify all information, eligibility requirements and submission instructions against the official government form/instructions before submitting.";
    private boolean documentStored = false;
    private String message;

    public FormScanResponse() {
    }

    public static FormScanResponse notConfigured(String message) {
        FormScanResponse resp = new FormScanResponse();
        resp.setSuccess(false);
        resp.setSource("NOT_CONFIGURED");
        resp.setDocumentType("UNKNOWN");
        resp.setDocumentTypeConfidence(0);
        resp.setSummary("Government Form Scanner is unconfigured on this server.");
        resp.setPageCount(0);
        resp.setMessage(message != null ? message : "Gemini API key is not configured. Form scanning requires Gemini multimodal document analysis.");
        resp.setDocumentStored(false);
        return resp;
    }

    public static FormScanResponse failure(String source, String message) {
        FormScanResponse resp = new FormScanResponse();
        resp.setSuccess(false);
        resp.setSource(source != null ? source : "UNAVAILABLE");
        resp.setDocumentType("UNKNOWN");
        resp.setDocumentTypeConfidence(0);
        resp.setSummary("Document analysis could not be completed.");
        resp.setPageCount(0);
        resp.setMessage(message);
        resp.setDocumentStored(false);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Integer getDocumentTypeConfidence() {
        return documentTypeConfidence;
    }

    public void setDocumentTypeConfidence(Integer documentTypeConfidence) {
        this.documentTypeConfidence = documentTypeConfidence;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public List<FormScanFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<FormScanFieldDto> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getVerificationItems() {
        return verificationItems;
    }

    public void setVerificationItems(List<String> verificationItems) {
        this.verificationItems = verificationItems != null ? verificationItems : new ArrayList<>();
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public boolean isDocumentStored() {
        return documentStored;
    }

    public void setDocumentStored(boolean documentStored) {
        this.documentStored = documentStored;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
