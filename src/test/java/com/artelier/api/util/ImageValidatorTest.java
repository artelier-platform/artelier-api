
package com.artelier.api.util;

import com.artelier.api.exception.ArtelierException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ImageValidator Unit Tests")
class ImageValidatorTest {

    private static final String EMPTY_FILE_MESSAGE =
            "Image file cannot be empty";

    private static final String MAX_SIZE_MESSAGE =
            "Each image must not exceed 5 MB";

    private static final String INVALID_CONTENT_TYPE_MESSAGE =
            "Only image files are allowed";

    private static final String UNSUPPORTED_FORMAT_MESSAGE =
            "Unsupported image format. Use JPG, PNG, WEBP, or GIF";

    private static final String READ_ERROR_MESSAGE =
            "Unable to read the image file";

    private static final long VALID_FILE_SIZE = 1024L;

    @Nested
    @DisplayName("File Presence Validation")
    class FilePresenceValidationTests {

        @Test
        @DisplayName("Should throw BAD_REQUEST when file is null")
        void shouldRejectNullFile() {
            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(null)
            );

            assertBadRequest(exception, EMPTY_FILE_MESSAGE);
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when file is empty")
        void shouldRejectEmptyFile() {
            MultipartFile file = new MockMultipartFile(
                    "image",
                    "empty.png",
                    "image/png",
                    new byte[0]
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(exception, EMPTY_FILE_MESSAGE);
        }
    }

    @Nested
    @DisplayName("File Size Validation")
    class FileSizeValidationTests {

        @Test
        @DisplayName("Should reject files larger than 5 MB")
        void shouldRejectFileLargerThanMaximumSize() {
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(
                    ImageValidator.MAX_BYTES + 1
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(exception, MAX_SIZE_MESSAGE);

            verify(file, never()).getContentType();
            try {
                verify(file, never()).getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Should accept files exactly 5 MB in size")
        void shouldAcceptFileAtMaximumSize() {
            MultipartFile file = mockValidImage(
                    "image/png",
                    pngHeader()
            );

            when(file.getSize()).thenReturn(ImageValidator.MAX_BYTES);

            assertDoesNotThrow(() -> ImageValidator.validate(file));
        }

        @Test
        @DisplayName("Should accept files smaller than 5 MB")
        void shouldAcceptFileSmallerThanMaximumSize() {
            MultipartFile file = mockValidImage(
                    "image/png",
                    pngHeader()
            );

            when(file.getSize()).thenReturn(VALID_FILE_SIZE);

            assertDoesNotThrow(() -> ImageValidator.validate(file));
        }
    }

    @Nested
    @DisplayName("Content Type Validation")
    class ContentTypeValidationTests {

        @Test
        @DisplayName("Should reject a null content type")
        void shouldRejectNullContentType() {
            MultipartFile file = mockFileWithoutReading();

            when(file.getContentType()).thenReturn(null);

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    INVALID_CONTENT_TYPE_MESSAGE
            );

            try {
                verify(file, never()).getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Should reject a non-image content type")
        void shouldRejectNonImageContentType() {
            MultipartFile file = mockFileWithoutReading();

            when(file.getContentType()).thenReturn(
                    "application/pdf"
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    INVALID_CONTENT_TYPE_MESSAGE
            );

            try {
                verify(file, never()).getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Should reject an empty content type")
        void shouldRejectEmptyContentType() {
            MultipartFile file = mockFileWithoutReading();

            when(file.getContentType()).thenReturn("");

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    INVALID_CONTENT_TYPE_MESSAGE
            );
        }

        @Test
        @DisplayName("Should reject a content type without the image prefix")
        void shouldRejectContentTypeWithoutImagePrefix() {
            MultipartFile file = mockFileWithoutReading();

            when(file.getContentType()).thenReturn("text/image");

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    INVALID_CONTENT_TYPE_MESSAGE
            );
        }

        @Test
        @DisplayName("Should accept an uppercase image content type")
        void shouldAcceptUppercaseImageContentType() {
            MultipartFile file = mockValidImage(
                    "IMAGE/PNG",
                    pngHeader()
            );

            assertDoesNotThrow(() -> ImageValidator.validate(file));
        }
    }

    @Nested
    @DisplayName("Image Signature Validation")
    class ImageSignatureValidationTests {

        @Test
        @DisplayName("Should accept a valid JPEG signature")
        void shouldAcceptJpegSignature() {
            assertValidImage(
                    "image/jpeg",
                    new byte[]{
                            (byte) 0xFF,
                            (byte) 0xD8,
                            (byte) 0xFF,
                            0, 0, 0, 0, 0, 0, 0, 0, 0
                    }
            );
        }

        @Test
        @DisplayName("Should accept a valid PNG signature")
        void shouldAcceptPngSignature() {
            assertValidImage("image/png", pngHeader());
        }

        @Test
        @DisplayName("Should accept a valid GIF signature")
        void shouldAcceptGifSignature() {
            assertValidImage(
                    "image/gif",
                    new byte[]{
                            'G', 'I', 'F', '8', '9', 'a',
                            0, 0, 0, 0, 0, 0
                    }
            );
        }

        @Test
        @DisplayName("Should accept a valid WEBP signature")
        void shouldAcceptWebpSignature() {
            assertValidImage(
                    "image/webp",
                    new byte[]{
                            'R', 'I', 'F', 'F',
                            0, 0, 0, 0,
                            'W', 'E', 'B', 'P'
                    }
            );
        }

        @Test
        @DisplayName("Should reject an unsupported image signature")
        void shouldRejectUnsupportedImageSignature() {
            MultipartFile file = mockValidImage(
                    "image/png",
                    new byte[]{
                            0, 1, 2, 3, 4, 5,
                            6, 7, 8, 9, 10, 11
                    }
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    UNSUPPORTED_FORMAT_MESSAGE
            );
        }

        @Test
        @DisplayName("Should reject a header shorter than 12 bytes")
        void shouldRejectShortHeader() {
            MultipartFile file = mockValidImage(
                    "image/png",
                    new byte[]{
                            (byte) 0x89, 'P', 'N', 'G'
                    }
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    UNSUPPORTED_FORMAT_MESSAGE
            );
        }

        @Test
        @DisplayName("Should reject a JPEG signature with an invalid prefix")
        void shouldRejectInvalidJpegSignature() {
            assertInvalidImageSignature(
                    "image/jpeg",
                    new byte[]{
                            (byte) 0xFF,
                            (byte) 0xD8,
                            0x00,
                            0, 0, 0, 0, 0, 0, 0, 0, 0
                    }
            );
        }

        @Test
        @DisplayName("Should reject a PNG signature with an invalid prefix")
        void shouldRejectInvalidPngSignature() {
            assertInvalidImageSignature(
                    "image/png",
                    new byte[]{
                            0x00, 'P', 'N', 'G',
                            13, 10, 26, 10,
                            0, 0, 0, 0
                    }
            );
        }

        @Test
        @DisplayName("Should reject a GIF signature with an invalid prefix")
        void shouldRejectInvalidGifSignature() {
            assertInvalidImageSignature(
                    "image/gif",
                    new byte[]{
                            'G', 'I', 'F', '7', '9', 'a',
                            0, 0, 0, 0, 0, 0
                    }
            );
        }

        @Test
        @DisplayName("Should reject a WEBP signature with an invalid identifier")
        void shouldRejectInvalidWebpSignature() {
            assertInvalidImageSignature(
                    "image/webp",
                    new byte[]{
                            'R', 'I', 'F', 'X',
                            0, 0, 0, 0,
                            'W', 'E', 'B', 'P'
                    }
            );
        }
    }

    @Nested
    @DisplayName("Input Stream Error Handling")
    class InputStreamErrorHandlingTests {

        @Test
        @DisplayName("Should convert IOException into BAD_REQUEST")
        void shouldHandleInputStreamIOException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(VALID_FILE_SIZE);
            when(file.getContentType()).thenReturn("image/png");
            when(file.getInputStream()).thenThrow(
                    new IOException("Read failure")
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(exception, READ_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should reject a stream that returns no bytes")
        void shouldRejectEmptyInputStream() throws IOException {
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(VALID_FILE_SIZE);
            when(file.getContentType()).thenReturn("image/png");
            when(file.getInputStream()).thenReturn(
                    new ByteArrayInputStream(new byte[0])
            );

            ArtelierException exception = assertThrows(
                    ArtelierException.class,
                    () -> ImageValidator.validate(file)
            );

            assertBadRequest(
                    exception,
                    UNSUPPORTED_FORMAT_MESSAGE
            );
        }
    }

    private MultipartFile mockFileWithoutReading() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(VALID_FILE_SIZE);

        return file;
    }

    private MultipartFile mockValidImage(
            String contentType,
            byte[] header
    ) {
        MultipartFile file = mockFileWithoutReading();

        when(file.getContentType()).thenReturn(contentType);

        try {
            when(file.getInputStream()).thenReturn(
                    new ByteArrayInputStream(header)
            );
        } catch (IOException exception) {
            throw new AssertionError(
                    "Unable to configure the mock input stream",
                    exception
            );
        }

        return file;
    }

    private void assertValidImage(
            String contentType,
            byte[] header
    ) {
        MultipartFile file = mockValidImage(contentType, header);

        assertDoesNotThrow(() -> ImageValidator.validate(file));
    }

    private void assertInvalidImageSignature(
            String contentType,
            byte[] header
    ) {
        MultipartFile file = mockValidImage(contentType, header);

        ArtelierException exception = assertThrows(
                ArtelierException.class,
                () -> ImageValidator.validate(file)
        );

        assertBadRequest(exception, UNSUPPORTED_FORMAT_MESSAGE);
    }

    private void assertBadRequest(
            ArtelierException exception,
            String expectedMessage
    ) {
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private byte[] pngHeader() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G',
                13, 10, 26, 10,
                0, 0, 0, 0
        };
    }
}
