package com.framework.crud.exception;

import com.framework.crud.model.CrudResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the CRUD framework.
 * Converts framework exceptions into consistent {@link CrudResponse} objects.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<CrudResponse> handleDuplicate(DuplicateEntityException ex) {
        log.warn("Duplicate entity: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CrudValidationException.class)
    public ResponseEntity<CrudResponse> handleValidation(CrudValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CrudResponse.error(ex.getMessage(), ex.getFieldErrors()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CrudResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CrudAccessDeniedException.class)
    public ResponseEntity<CrudResponse> handleAccessDenied(CrudAccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedEntityException.class)
    public ResponseEntity<CrudResponse> handleUnsupportedEntity(UnsupportedEntityException ex) {
        log.warn("Unsupported entity/operation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CrudException.class)
    public ResponseEntity<CrudResponse> handleCrudException(CrudException ex) {
        log.error("CRUD error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CrudResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CrudResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CrudResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CrudResponse.error("An unexpected error occurred"));
    }
}
