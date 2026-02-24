package com.framework.crud.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of a validation operation.
 * <p>
 * Contains a success flag and a map of field-level errors.
 * The framework's built-in validation populates this with structural checks
 * (required fields, max-length, patterns, etc.), and the developer's custom
 * validation method in {@code EntityDefinition} can add additional errors.
 */
public class ValidationResult {

    private boolean valid;
    private final Map<String, String> errors;

    private ValidationResult(boolean valid, Map<String, String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    /** Create a successful validation result. */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyMap());
    }

    /** Create from a pre-built error map. */
    public static ValidationResult failure(Map<String, String> errors) {
        return new ValidationResult(false, errors != null ? errors : Collections.emptyMap());
    }

    /** Start building a result to which errors can be added. */
    public static ValidationResult builder() {
        return new ValidationResult(true, new LinkedHashMap<>());
    }

    /**
     * Add a field error. Automatically marks the result as invalid.
     */
    public ValidationResult addError(String field, String message) {
        this.errors.put(field, message);
        this.valid = false;
        return this;
    }

    /**
     * Merge another ValidationResult's errors into this one.
     */
    public ValidationResult merge(ValidationResult other) {
        if (other != null && !other.isValid()) {
            other.getErrors().forEach(this::addError);
        }
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }
}
