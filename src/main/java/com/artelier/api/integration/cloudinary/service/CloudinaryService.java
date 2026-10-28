package com.artelier.api.integration.cloudinary.service;

import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String RESOURCE_IMAGE = "image";
    String RESOURCE_RAW = "raw";

    /** Sube a la carpeta de productos (comportamiento anterior). */
    CloudinaryUploadResponse upload(MultipartFile file);

    CloudinaryUploadResponse upload(MultipartFile file, String folder);

    /** Borra un archivo. No lanza excepción: si falla, solo registra el error. */
    void delete(String publicId, String resourceType);
}
