package com.smartshield.dto;

import java.time.LocalDateTime;

import com.smartshield.model.GrievanceEvidence;

public class GrievanceEvidenceResponse {

    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    public GrievanceEvidenceResponse() {
    }

    public GrievanceEvidenceResponse(Long id, String originalFileName, String contentType,
                                   Long fileSize, LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public static GrievanceEvidenceResponse fromEntity(GrievanceEvidence evidence) {
        if (evidence == null) {
            return null;
        }
        return new GrievanceEvidenceResponse(
                evidence.getId(),
                evidence.getOriginalFileName(),
                evidence.getContentType(),
                evidence.getFileSize(),
                evidence.getUploadedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
