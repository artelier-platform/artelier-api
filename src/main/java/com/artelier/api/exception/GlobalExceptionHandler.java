package com.artelier.api.exception;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.integration.wompi.exception.WompiException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

    @ExceptionHandler(ArtelierException.class)
    public ResponseEntity<AppResponse<Void>> handleArtelierException(ArtelierException ex) {
        return ResponseEntity.status(ex.getStatus()).body(AppResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of(
                        "field", e.getField(),
                        "message", StringUtils.hasText(e.getDefaultMessage())
                                ? e.getDefaultMessage()
                                : "Invalid value"
                ))
                .toList();
        return ResponseEntity.badRequest()
                .body(new AppResponse<>(false, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AppResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AppResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Access denied";

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AppResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AppResponse.error("Something went wrong"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AppResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        log.error("JSON parsing error", ex);
        String errorMessage = "Invalid request body. Please check JSON format.";
        ex.getMostSpecificCause();
        if (ex.getMostSpecificCause().getMessage() != null) {
            log.debug("Detailed cause: {}",
                    ex.getMostSpecificCause().getMessage());
        }
        return ResponseEntity.badRequest()
                .body(AppResponse.error(errorMessage));
    }

    @ExceptionHandler(WompiException.class)
    public ResponseEntity<AppResponse<Object>> handleWompiException(
            WompiException ex) {

        return ResponseEntity.badRequest()
                .body(new AppResponse<>(
                        false,
                        ex.getMessage(),
                        Map.of(
                                "provider", "WOMPI",
                                "code", ex.getWompiCode()
                        )
                ));
    }
}
