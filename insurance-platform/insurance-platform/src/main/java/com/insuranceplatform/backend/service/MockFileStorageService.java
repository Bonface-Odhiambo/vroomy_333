package com.insuranceplatform.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Slf4j
public class MockFileStorageService implements FileStorageService {

    @Override
    public String storeFile(MultipartFile file, String bucketName) {
        // This is a mock service. It doesn't actually store the file.
        // It just logs the action and returns a fake URL.
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String fakeUrl = String.format("/uploads/%s/%s", bucketName, fileName);

        log.info("MOCK UPLOAD: Storing file '{}' to bucket '{}'. Returning fake URL: {}",
                file.getOriginalFilename(), bucketName, fakeUrl);

        return fakeUrl;
    }
}