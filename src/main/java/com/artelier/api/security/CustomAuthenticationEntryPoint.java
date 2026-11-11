package com.artelier.api.security;

import com.artelier.api.dto.response.AppResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom implementation of Spring Security's {@link AuthenticationEntryPoint}
 * responsible for handling unauthorized access attempts.
 *
 * <p>Intercepts authentication exceptions, logs a warning message with the requested URI, 
 * and writes a standardized JSON error response with an HTTP 401 Unauthorized status.</p>
 */
@Slf4j
@AllArgsConstructor
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Commences the authentication failure handling process.
     *
     * <p>Configures the HTTP response with a 401 Unauthorized status code, JSON content type, 
     * UTF-8 encoding, and serializes a standard error payload using the provided object mapper.</p>
     *
     * @param request       the current HTTP request that resulted in an authentication failure
     * @param response      the HTTP response used to write the error payload
     * @param authException the exception thrown due to invalid or missing credentials
     * @throws IOException  if an I/O error occurs while writing to the response output stream
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AuthenticationException authException
    ) throws IOException {

        log.warn("Unauthorized access attempt to {}", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                AppResponse.error("Authentication required")
        );
    }
}