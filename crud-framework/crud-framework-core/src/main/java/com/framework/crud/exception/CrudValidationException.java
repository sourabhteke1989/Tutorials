package com.framework.crud.exception;

import java.util.Map;

/**
 * Thrown when payload validation fails.
 * Carries a map of field → error message.
 */
public class CrudValidationException extends CrudException {

    private final Map<String, String> fieldErrors;

    public CrudValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_FAILED", "Payload validation failed");
        this.fieldErrors = fieldErrors;
    }

    public CrudValidationException(String message, Map<String, String> fieldErrors) {
        super("VALIDATION_FAILED", message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
