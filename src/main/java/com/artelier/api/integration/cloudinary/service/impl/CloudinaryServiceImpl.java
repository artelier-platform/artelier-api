package com.artelier.api.integration.cloudinary.service.impl;

import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.exception.CloudinaryUploadException;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@AllArgsConstructor
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;

    @Override
    public CloudinaryUploadResponse upload(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CloudinaryUploadException(
                    "Failed to read file bytes before upload: " + file.getOriginalFilename(), e);
        }

        try {
            var result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap("folder", "artelier/products")
            );

            return objectMapper.convertValue(result, CloudinaryUploadResponse.class);

        } catch (Exception e) {
            throw new CloudinaryUploadException(
                    "Cloudinary upload failed for file: " + file.getOriginalFilename(), e);
        }
    }
}