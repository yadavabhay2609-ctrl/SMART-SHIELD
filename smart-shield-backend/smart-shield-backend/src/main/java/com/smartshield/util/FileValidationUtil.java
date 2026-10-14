package com.smartshield.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reusable server-side file validation utility with directory traversal guards,
 * MIME type checking, extension verification, and size restrictions.
 */
@Component
public class FileValidationUtil {

    // Default maximum file size: 10 MB
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    // Allowed MIME types for public grievances and evidence
    public static final Set<String> DEFAULT_EVIDENCE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    // Allowed extensions for public grievances and evidence
    public static final Set<String> DEFAULT_EVIDENCE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp",
            ".pdf"
    );

    /**
     * Validates an uploaded evidence file against default grievance rules (10MB, images + PDF),
     * including path safety, extension, client MIME, and header magic-byte verification.
     *
     * @param file the uploaded file
     * @return the verified canonical MIME type
     * @throws IllegalArgumentException if validation fails
     */
    public String validateEvidenceFile(MultipartFile file) {
        validateFile(file, DEFAULT_MAX_FILE_SIZE_BYTES, DEFAULT_EVIDENCE_MIME_TYPES, DEFAULT_EVIDENCE_EXTENSIONS);
        String detectedMime = validateMagicBytes(file);

        String ext = getFileExtension(file.getOriginalFilename());
        if ((".jpg".equals(ext) || ".jpeg".equals(ext)) && !"image/jpeg".equals(detectedMime)) {
            throw new IllegalArgumentException("File extension does not match detected file signature (" + detectedMime + ")");
        }
        if (".png".equals(ext) && !"image/png".equals(detectedMime)) {
            throw new IllegalArgumentException("File extension does not match detected file signature (" + detectedMime + ")");
        }
        if (".webp".equals(ext) && !"image/webp".equals(detectedMime)) {
            throw new IllegalArgumentException("File extension does not match detected file signature (" + detectedMime + ")");
        }
        if (".pdf".equals(ext) && !"application/pdf".equals(detectedMime)) {
            throw new IllegalArgumentException("File extension does not match detected file signature (" + detectedMime + ")");
        }

        return detectedMime;
    }

    /**
     * Inspects the initial bytes of a file to verify its true format against allowed magic bytes.
     *
     * Supported formats:
     * - JPEG: FF D8 FF
     * - PNG:  89 50 4E 47 0D 0A 1A 0A
     * - WebP: RIFF (bytes 0-3) and WEBP (bytes 8-11)
     * - PDF:  %PDF (bytes 0-3: 25 50 44 46)
     *
     * @param file the uploaded multipart file
     * @return the verified canonical MIME type ("image/jpeg", "image/png", "image/webp", "application/pdf")
     * @throws IllegalArgumentException if the file is empty, truncated, or has an unrecognized signature
     */
    public String validateMagicBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be null or empty");
        }

        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.readNBytes(header, 0, 12);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file header for signature validation", e);
        }

        if (bytesRead < 3) {
            throw new IllegalArgumentException("File is corrupted or too small to contain a valid signature");
        }

        // 1. JPEG signature: FF D8 FF
        if (bytesRead >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // 2. PNG signature: 89 50 4E 47 0D 0A 1A 0A
        if (bytesRead >= 8
                && (header[0] & 0xFF) == 0x89
                && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x4E
                && (header[3] & 0xFF) == 0x47
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A) {
            return "image/png";
        }

        // 3. WebP signature: bytes 0-3 = "RIFF" (52 49 46 46), bytes 8-11 = "WEBP" (57 45 42 50)
        if (bytesRead >= 12
                && (header[0] & 0xFF) == 0x52
                && (header[1] & 0xFF) == 0x49
                && (header[2] & 0xFF) == 0x46
                && (header[3] & 0xFF) == 0x46
                && (header[8] & 0xFF) == 0x57
                && (header[9] & 0xFF) == 0x45
                && (header[10] & 0xFF) == 0x42
                && (header[11] & 0xFF) == 0x50) {
            return "image/webp";
        }

        // 4. PDF signature: bytes 0-3 = "%PDF" (25 50 44 46)
        if (bytesRead >= 4
                && (header[0] & 0xFF) == 0x25
                && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x44
                && (header[3] & 0xFF) == 0x46) {
            return "application/pdf";
        }

        throw new IllegalArgumentException("File content signature does not match any allowed evidence format (JPEG, PNG, WebP, PDF)");
    }

    /**
     * Validates a file against specified size, MIME types, and extension rules with path safety checks.
     *
     * @param file the uploaded multipart file
     * @param maxSizeBytes maximum permitted size in bytes
     * @param allowedMimes set of permitted MIME content types
     * @param allowedExtensions set of permitted file extensions (e.g. .jpg, .pdf)
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public void validateFile(MultipartFile file,
                             long maxSizeBytes,
                             Set<String> allowedMimes,
                             Set<String> allowedExtensions) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be null or empty");
        }

        // 1. File size validation
        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException("File size exceeds maximum permitted limit of " + maxMb + " MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }

        // 2. Directory traversal & path safety check
        if (!isPathTraversalSafe(originalFilename)) {
            throw new IllegalArgumentException("Filename contains invalid or dangerous path traversal characters");
        }

        // 3. Extension validation
        String extension = getFileExtension(originalFilename);
        if (extension.isEmpty() || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("File extension '" + extension + "' is not permitted. Allowed: " + allowedExtensions);
        }

        // 4. Content-Type / MIME validation
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Content-Type '" + contentType + "' is not permitted. Allowed: " + allowedMimes);
        }
    }

    /**
     * Checks if a filename contains path traversal sequences ('..', '/', '\\') or null bytes.
     */
    public boolean isPathTraversalSafe(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        // Check for null bytes or relative path indicators
        if (filename.contains("\0") || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Ensure sanitized base name matches input
        String baseName = Paths.get(filename).getFileName().toString();
        return baseName.equals(filename);
    }

    /**
     * Extracts lowercase file extension including dot (e.g. ".png").
     */
    public String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
    }

    /**
     * Sanitizes a filename by replacing any non-alphanumeric character (except dot, dash, underscore) with an underscore.
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "";
        }
        String nameOnly = Paths.get(filename).getFileName().toString();
        return nameOnly.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Generates a collision-free UUID filename while retaining the verified extension.
     */
    public String generateSafeFileName(String originalFilename) {
        String ext = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + ext;
    }
}
