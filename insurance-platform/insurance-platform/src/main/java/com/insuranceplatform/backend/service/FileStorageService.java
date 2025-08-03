package com.insuranceplatform.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Stores a file in a specified bucket/folder.
     *
     * @param file The file to store.
     * @param bucketName The logical bucket or folder name (e.g., "documents", "claims").
     * @return The public URL or path to the stored file.
     */
    String storeFile(MultipartFile file, String bucketName);
}