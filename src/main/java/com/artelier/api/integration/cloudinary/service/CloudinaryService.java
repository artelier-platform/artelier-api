package com.artelier.api.integration.cloudinary.service;

import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    CloudinaryUploadResponse upload(MultipartFile file);
}