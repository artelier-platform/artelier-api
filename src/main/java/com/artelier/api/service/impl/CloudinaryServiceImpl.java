package com.artelier.api.service.impl;

import com.artelier.api.service.CloudinaryService;
import com.cloudinary.Cloudinary;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@AllArgsConstructor
@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> upload(MultipartFile file) {
        try {
            return (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of("folder", "artelier/products")
            );
        } catch (Exception e) {
            throw new RuntimeException("Error uploading image", e);
        }
    }
}
