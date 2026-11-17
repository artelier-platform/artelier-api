package com.artelier.api.auth.security;

import com.artelier.api.shared.dto.AppResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom implementation of Spring Security's {@link AccessDeniedHandler}
 * responsible for handling forbidden access attempts by authenticated users lacking required permissions.
 *
 * <p>Intercepts access denied exceptions, logs a warning message with the requested URI, 
 * and writes a standardized JSON error response with an HTTP 403 Forbidden status.</p>
 */
@Slf4j
@AllArgsConstructor
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handles an access denial failure when an authenticated user attempts to access a protected resource 
     * without sufficient privileges or authorities.
     *
     * <p>Configures the HTTP response with a 403 Forbidden status code, JSON content type, 
     * UTF-8 encoding, and serializes a standard error payload using the provided object mapper.</p>
     *
     * @param request               the current HTTP request that resulted in an access denial
     * @param response              the HTTP response used to write the error payload
     * @param accessDeniedException the exception thrown due to insufficient authorization
     * @throws IOException          if an I/O error occurs while writing to the response output stream
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AccessDeniedException accessDeniedException
    ) throws IOException {

        log.warn("Forbidden access attempt to {}", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                AppResponse.error("Access denied")
        );
    }
}