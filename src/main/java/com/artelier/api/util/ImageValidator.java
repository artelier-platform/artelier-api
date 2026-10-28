package com.artelier.api.util;

import com.artelier.api.exception.ArtelierException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class ImageValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ImageValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ArtelierException.badRequest(
                    "Image file cannot be empty"
            );
        }

        if (file.getSize() > MAX_BYTES) {
            throw ArtelierException.badRequest(
                    "Each image must not exceed 5 MB"
            );
        }

        String type = file.getContentType();

        if (type == null
                || !type.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw ArtelierException.badRequest(
                    "Only image files are allowed"
            );
        }

        // Content-Type is provided by the client, so we also
        // validate the file signature using its initial bytes.
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);

            if (!isKnownImage(header)) {
                throw ArtelierException.badRequest(
                        "Unsupported image format. "
                                + "Use JPG, PNG, WEBP, or GIF"
                );
            }
        } catch (IOException e) {
            throw ArtelierException.badRequest(
                    "Unable to read the image file"
            );
        }
    }

    private static boolean isKnownImage(byte[] header) {
        if (header.length < 12) {
            return false;
        }

        boolean jpeg =
                (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;

        boolean png =
                (header[0] & 0xFF) == 0x89
                        && header[1] == 'P'
                        && header[2] == 'N'
                        && header[3] == 'G';

        boolean gif =
                header[0] == 'G'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == '8';

        boolean webp =
                header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';

        return jpeg || png || gif || webp;
    }
}