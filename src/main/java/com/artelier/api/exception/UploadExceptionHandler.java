package com.artelier.api.exception;

import com.artelier.api.dto.response.AppResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Errores de subida de archivos. Va con prioridad máxima porque GlobalExceptionHandler
 * captura Exception y, si se evaluara primero, devolvería 500 para estos casos.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<AppResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(AppResponse.error("File is too large"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<AppResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AppResponse.error("Missing required part '" + ex.getRequestPartName() + "'"));
    }
}
