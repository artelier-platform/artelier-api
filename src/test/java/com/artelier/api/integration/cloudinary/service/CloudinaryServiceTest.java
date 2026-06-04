package com.artelier.api.integration.cloudinary.service;

import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.exception.CloudinaryUploadException;
import com.artelier.api.integration.cloudinary.service.impl.CloudinaryServiceImpl;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class CloudinaryServiceTest {

    private Uploader uploader;
    private CloudinaryServiceImpl service;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        service = new CloudinaryServiceImpl(cloudinary, new ObjectMapper());
        file = mock(MultipartFile.class);

        when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ─────────────────────────────────────────────
    // upload — happy path
    // ─────────────────────────────────────────────

    @Test
    void shouldUploadFileSuccessfully() throws Exception {
        when(file.getBytes()).thenReturn("test".getBytes());
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/artelier/image/upload/abc.jpg",
                "public_id",  "artelier/products/abc"
        ));

        CloudinaryUploadResponse result = service.upload(file);

        assertNotNull(result);
        assertEquals("https://res.cloudinary.com/artelier/image/upload/abc.jpg", result.getSecureUrl());
        assertEquals("artelier/products/abc", result.getPublicId());
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    // ─────────────────────────────────────────────
    // upload — file read failure
    // ─────────────────────────────────────────────

    @Test
    void shouldThrowCloudinaryUploadExceptionIfFileReadFails() throws Exception {
        when(file.getBytes()).thenThrow(new IOException("disk error"));
        when(file.getOriginalFilename()).thenReturn("image.jpg");

        CloudinaryUploadException ex = assertThrows(CloudinaryUploadException.class,
                () -> service.upload(file));

        assertTrue(ex.getMessage().contains("Failed to read file bytes before upload"));
        assertTrue(ex.getMessage().contains("image.jpg"));
    }

    // ─────────────────────────────────────────────
    // upload — Cloudinary API failure
    // ─────────────────────────────────────────────

    @Test
    void shouldThrowCloudinaryUploadExceptionIfApiCallFails() throws Exception {
        when(file.getBytes()).thenReturn("test".getBytes());
        when(file.getOriginalFilename()).thenReturn("image.jpg");
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new RuntimeException("Cloudinary timeout"));

        CloudinaryUploadException ex = assertThrows(CloudinaryUploadException.class,
                () -> service.upload(file));

        assertTrue(ex.getMessage().contains("Cloudinary upload failed for file"));
        assertTrue(ex.getMessage().contains("image.jpg"));
    }
}