package com.smartshield.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartshield.util.FileValidationUtil;

/**
 * Service providing secure server-side storage and management for grievance evidence files.
 * Enforces magic-byte validation, path traversal guards, UUID-based file isolation,
 * and memory-safe streaming storage.
 */
@Service
public class GrievanceEvidenceStorageService {

    private final FileValidationUtil fileValidationUtil;
    private final String configuredDir;
    private final Path evidenceRootPath;

    public GrievanceEvidenceStorageService(
            FileValidationUtil fileValidationUtil,
            @Value("${smartshield.evidence.storage-dir:uploads/evidence}") String configuredDir) {
        this.fileValidationUtil = fileValidationUtil;
        this.configuredDir = configuredDir;
        this.evidenceRootPath = Paths.get(this.configuredDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.evidenceRootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize evidence storage directory: " + this.evidenceRootPath, e);
        }
    }

    /**
     * Stores an uploaded grievance evidence file after comprehensive validation.
     *
     * @param file the uploaded multipart file
     * @return StoredEvidence containing file metadata for creating GrievanceEvidence records
     * @throws IllegalArgumentException if the file fails validation (empty, size, extension, MIME, magic-bytes)
     * @throws SecurityException if path traversal is detected
     * @throws IOException if saving the file stream to disk fails
     */
    public StoredEvidence storeEvidence(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence file cannot be null or empty");
        }

        // 1. Run size, extension, MIME, path-traversal, and magic-byte validations
        String validatedContentType = fileValidationUtil.validateEvidenceFile(file);

        // 2. Generate a secure collision-free UUID filename retaining the verified extension
        String originalFilename = file.getOriginalFilename();
        String safeOriginalName = (originalFilename != null && !originalFilename.isBlank())
                ? fileValidationUtil.sanitizeFilename(originalFilename)
                : "evidence";
        String storedFileName = fileValidationUtil.generateSafeFileName(safeOriginalName);

        // 3. Resolve destination path and verify bounds against traversal
        Path destinationPath = this.evidenceRootPath.resolve(storedFileName).normalize();
        if (!destinationPath.startsWith(this.evidenceRootPath)) {
            throw new SecurityException("Path traversal attempt detected while resolving storage path");
        }

        // 4. Safely stream file directly to disk without buffering complete file into memory
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 5. Construct normalized relative path for database persistence
        String relativePath = this.configuredDir.replace('\\', '/') + "/" + storedFileName;

        return new StoredEvidence(
                storedFileName,
                safeOriginalName,
                relativePath,
                validatedContentType,
                file.getSize()
        );
    }

    /**
     * Safely deletes an evidence file from the storage directory.
     * Ensures the file path does not escape the evidence root directory.
     *
     * @param fileNameOrPath the stored filename or relative path
     * @return true if the file was deleted, false if it did not exist
     * @throws SecurityException if path traversal outside the evidence root is attempted
     * @throws IOException if an I/O error occurs during deletion
     */
    public boolean deleteEvidence(String fileNameOrPath) throws IOException {
        if (fileNameOrPath == null || fileNameOrPath.isBlank()) {
            return false;
        }

        Path targetPath = this.evidenceRootPath.resolve(fileNameOrPath).normalize();

        // Enforce path traversal protection
        if (!targetPath.startsWith(this.evidenceRootPath)) {
            throw new SecurityException("Path traversal attempt detected during evidence deletion: " + fileNameOrPath);
        }

        // Refuse deletion of the evidence root itself
        if (targetPath.equals(this.evidenceRootPath)) {
            throw new SecurityException("Cannot delete the evidence root directory");
        }

        return Files.deleteIfExists(targetPath);
    }

    /**
     * Resolves an evidence file path safely within the evidence root.
     *
     * @param fileNameOrPath the stored filename or relative path
     * @return normalized Path inside the evidence directory
     * @throws SecurityException if path traversal is attempted
     */
    public Path loadEvidencePath(String fileNameOrPath) {
        if (fileNameOrPath == null || fileNameOrPath.isBlank()) {
            throw new IllegalArgumentException("Evidence filename cannot be null or empty");
        }

        Path targetPath = this.evidenceRootPath.resolve(fileNameOrPath).normalize();
        if (!targetPath.startsWith(this.evidenceRootPath) || targetPath.equals(this.evidenceRootPath)) {
            throw new SecurityException("Path traversal attempt detected: " + fileNameOrPath);
        }

        return targetPath;
    }

    /**
     * Returns the normalized absolute Path of the evidence root directory.
     */
    public Path getEvidenceRootPath() {
        return this.evidenceRootPath;
    }

    /**
     * Value record holding stored evidence file metadata.
     */
    public record StoredEvidence(
            String storedFileName,
            String originalFileName,
            String filePath,
            String contentType,
            long fileSize
    ) {
        public String getStoredFileName() { return storedFileName; }
        public String getOriginalFileName() { return originalFileName; }
        public String getFilePath() { return filePath; }
        public String getContentType() { return contentType; }
        public long getFileSize() { return fileSize; }
    }
}
