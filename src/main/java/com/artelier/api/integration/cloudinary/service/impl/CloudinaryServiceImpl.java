package com.artelier.api.integration.cloudinary.service.impl;

import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.exception.CloudinaryUploadException;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final String PRODUCTS_FOLDER = "artelier/products";

    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;

    @Override
    public CloudinaryUploadResponse upload(MultipartFile file) {
        return upload(file, PRODUCTS_FOLDER);
    }

    @Override
    public CloudinaryUploadResponse upload(MultipartFile file, String folder) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CloudinaryUploadException(
                    "Failed to read file bytes before upload: " + file.getOriginalFilename(), e);
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap("folder", folder)
            );
            return objectMapper.convertValue(result, CloudinaryUploadResponse.class);
        } catch (IOException | RuntimeException e) {
            throw new CloudinaryUploadException(
                    "Cloudinary upload failed for file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "invalidate", true)
            );
            log.info("Cloudinary asset {} ({}) deleted", publicId, resourceType);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete Cloudinary asset {} ({}): {}",
                    publicId, resourceType, e.getMessage());
        }
    }
}