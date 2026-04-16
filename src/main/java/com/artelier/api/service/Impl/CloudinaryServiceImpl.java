package com.artelier.api.service.Impl;

import com.artelier.api.service.CloudinaryService;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public Map upload(MultipartFile file) {
        try {
            return cloudinary.uploader().upload(file.getBytes(),
                    Map.of("folder", "artelier/products"));
        } catch (Exception e) {
            throw new RuntimeException("Error uploading image");
        }
    }
}
