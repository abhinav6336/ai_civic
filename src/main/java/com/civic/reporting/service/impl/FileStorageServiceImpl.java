package com.civic.reporting.service.impl;

import com.civic.reporting.exception.BadRequestException;
import com.civic.reporting.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path uploadLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads/issues}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("Issue uploads directory initialized at: {}", this.uploadLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize upload storage location", ex);
        }
    }

    @Override
    public String storeIssueImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate File Size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB.");
        }

        // Validate Content Type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Only image files (JPG, PNG, WebP) are allowed.");
        }

        // Validate Extension
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "photo.png"));
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid image file format. Allowed formats: JPG, JPEG, PNG, WebP.");
        }

        // Generate Unique Filename
        String baseName = (dotIndex > 0) ? originalFilename.substring(0, dotIndex).replaceAll("[^a-zA-Z0-9_-]", "_") : "photo";
        if (baseName.length() > 30) {
            baseName = baseName.substring(0, 30);
        }
        String uniqueFileName = UUID.randomUUID() + "-" + baseName + extension;

        try {
            // Check for relative path traversal
            if (uniqueFileName.contains("..")) {
                throw new BadRequestException("Invalid filename sequence: " + uniqueFileName);
            }

            Path targetPath = this.uploadLocation.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Saved uploaded issue photo: {}", targetPath);

            // Return relative URL to be stored in Issue.imageUrl
            return "/uploads/issues/" + uniqueFileName;

        } catch (IOException ex) {
            log.error("Failed to store file {}", uniqueFileName, ex);
            throw new BadRequestException("Could not store image file. Please try again.");
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/issues/")) {
            return false;
        }
        String fileName = fileUrl.replace("/uploads/issues/", "");
        try {
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Failed to delete file {}", fileUrl, ex);
            return false;
        }
    }
}
