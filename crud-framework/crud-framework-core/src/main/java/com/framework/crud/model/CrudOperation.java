package com.framework.crud.model;

/**
 * Supported CRUD operations.
 */
public enum CrudOperation {

    GET,
    CREATE,
    UPDATE,
    DELETE;

    /**
     * Parse a string to CrudOperation (case-insensitive).
     */
    public static CrudOperation fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Operation must not be null or blank");
        }
        try {
            return CrudOperation.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid operation: '" + value + "'. Supported: GET, CREATE, UPDATE, DELETE");
        }
    }
}
