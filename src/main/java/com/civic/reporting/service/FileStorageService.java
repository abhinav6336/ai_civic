package com.civic.reporting.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Stores an uploaded multipart file to the server-side uploads directory.
     * Validates file size, extension, and content type.
     * 
     * @param file the uploaded file
     * @return the relative URL path to access the file (e.g. /uploads/issues/uuid-name.png)
     */
    String storeIssueImage(MultipartFile file);

    /**
     * Deletes a file if needed.
     */
    boolean deleteFile(String fileUrl);
}
