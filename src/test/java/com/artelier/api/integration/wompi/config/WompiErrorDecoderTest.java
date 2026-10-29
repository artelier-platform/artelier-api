
package com.artelier.api.integration.wompi.config;

import com.artelier.api.integration.wompi.exception.WompiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class WompiErrorDecoderTest {

    private WompiErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        decoder = new WompiErrorDecoder(objectMapper);
    }

    private Response buildResponse(int status, String body) {
        return Response.builder()
                .status(status)
                .reason("Test reason")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/test",
                        Collections.emptyMap(),
                        null,
                        StandardCharsets.UTF_8,
                        null
                ))
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void decode_ShouldReturnInvalidKeyException_WhenStatusIs401() {
        // Arrange
        Response response = buildResponse(401, "{}");

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("INVALID_KEY");
        assertThat(wompiException.getMessage())
                .isEqualTo("Wompi authentication failed. Check your API keys.");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void decode_ShouldReturnUnprocessableContent_WhenStatusIs422() {
        // Arrange
        String body = """
                {
                    "error": {
                        "type": "INVALID_TRANSACTION",
                        "reason": "Transaction is invalid"
                    }
                }
                """;

        Response response = buildResponse(422, body);

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("INVALID_TRANSACTION");
        assertThat(wompiException.getMessage())
                .isEqualTo("Transaction is invalid");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void decode_ShouldReturnNotFound_WhenStatusIs404() {
        // Arrange
        Response response = buildResponse(404, "{}");

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("NOT_FOUND");
        assertThat(wompiException.getMessage())
                .isEqualTo("Wompi resource not found.");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void decode_ShouldReturnDefaultException_WhenStatusIs500() {
        // Arrange
        String body = """
                {
                    "error": {
                        "type": "SERVER_ERROR",
                        "reason": "Internal Wompi error"
                    }
                }
                """;

        Response response = buildResponse(500, body);

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("SERVER_ERROR");
        assertThat(wompiException.getMessage())
                .isEqualTo("Internal Wompi error");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void decode_ShouldUseDefaultValues_WhenErrorIsMissing() {
        // Arrange
        Response response = buildResponse(500, "{}");

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("UNKNOWN");
        assertThat(wompiException.getMessage())
                .isEqualTo("Wompi communication error");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void decode_ShouldUseMessagesAsFallback_WhenReasonIsMissing() {
        // Arrange
        String body = """
                {
                    "error": {
                        "type": "INVALID_REQUEST",
                        "messages": ["Invalid field", "Missing parameter"]
                    }
                }
                """;

        Response response = buildResponse(422, body);

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("INVALID_REQUEST");
        assertThat(wompiException.getMessage())
                .isEqualTo("[\"Invalid field\",\"Missing parameter\"]");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void decode_ShouldUseUnknownCode_WhenTypeIsMissing() {
        // Arrange
        String body = """
                {
                    "error": {
                        "reason": "Invalid request"
                    }
                }
                """;

        Response response = buildResponse(422, body);

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("UNKNOWN");
        assertThat(wompiException.getMessage())
                .isEqualTo("Invalid request");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void decode_ShouldHandleInvalidJson_WhenBodyCannotBeParsed() {
        // Arrange
        Response response = buildResponse(500, "invalid-json");

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("UNKNOWN");
        assertThat(wompiException.getMessage())
                .isEqualTo("Wompi communication error");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void decode_ShouldUseInternalServerError_WhenStatusIsUnknown() {
        // Arrange
        Response response = buildResponse(999, "{}");

        // Act
        Exception exception = decoder.decode("testMethod", response);

        // Assert
        assertThat(exception).isInstanceOf(WompiException.class);

        WompiException wompiException = (WompiException) exception;

        assertThat(wompiException.getWompiCode())
                .isEqualTo("UNKNOWN");
        assertThat(wompiException.getMessage())
                .isEqualTo("Wompi communication error");
        assertThat(wompiException.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
