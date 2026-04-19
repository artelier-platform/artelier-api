package com.artelier.api.service;

import com.artelier.api.service.Impl.CloudinaryServiceImpl;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryServiceTest {

    @Test
    void shouldUploadFile() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloudinary);

        MultipartFile file = mock(MultipartFile.class);

        when(file.getBytes()).thenReturn("test".getBytes());

        Map<String, Object> response = Map.of("url", "https://image.com");

        var uploader = mock(com.cloudinary.Uploader.class);

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(response);

        Map<String, Object> result = service.upload(file);

        assertEquals("https://image.com", result.get("url"));
    }

    @Test
    void shouldThrowOnError() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloudinary);

        MultipartFile file = mock(MultipartFile.class);

        when(file.getBytes()).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> service.upload(file));
    }
}