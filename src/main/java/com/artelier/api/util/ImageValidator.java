package com.artelier.api.util;

import com.artelier.api.exception.ArtelierException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public final class ImageValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ImageValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ArtelierException.badRequest("Archivo de imagen vacio");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ArtelierException.badRequest("Cada imagen debe pesar maximo 5 MB");
        }
        String type = file.getContentType();
        if (type == null || !type.toLowerCase().startsWith("image/")) {
            throw ArtelierException.badRequest("Solo se permiten imagenes");
        }
        // El content-type lo manda el cliente: se comprueban tambien los bytes iniciales.
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (!isKnownImage(h)) {
                throw ArtelierException.badRequest("Formato de imagen no soportado (usa JPG, PNG, WEBP o GIF)");
            }
        } catch (IOException e) {
            throw ArtelierException.badRequest("No se pudo leer la imagen");
        }
    }

    private static boolean isKnownImage(byte[] h) {
        if (h.length < 12) return false;
        boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean gif = h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
        boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        return jpeg || png || gif || webp;
    }
}
