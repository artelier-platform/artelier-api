package com.artelier.api.integration.wompi.config;

import com.artelier.api.integration.wompi.exception.WompiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class WompiErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        String wompiCode = "UNKNOWN";
        String message = "Wompi communication error";

        try (InputStream body = response.body().asInputStream()) {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                wompiCode = error.path("type").asText("UNKNOWN");
                message = error.path("reason").asText(
                        error.path("messages").toString()
                );
            }
        } catch (IOException e) {
            log.warn("Could not parse Wompi error body for method {}", methodKey);
        }

        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Wompi error [{}] {}: {} - {}", response.status(), methodKey, wompiCode, message);

        return switch (response.status()) {
            case 401 -> new WompiException("INVALID_KEY", "Wompi authentication failed. Check your API keys.", HttpStatus.INTERNAL_SERVER_ERROR);
            case 422 -> new WompiException(wompiCode, message, HttpStatus.UNPROCESSABLE_CONTENT);
            case 404 -> new WompiException("NOT_FOUND", "Wompi resource not found.", HttpStatus.NOT_FOUND);
            default  -> new WompiException(wompiCode, message, status);
        };
    }
}