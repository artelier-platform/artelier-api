package com.artelier.api.integration.cloudinary.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {
    Map<String, Object> upload(MultipartFile file);
}
