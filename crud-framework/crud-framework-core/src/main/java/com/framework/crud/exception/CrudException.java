package com.framework.crud.exception;

/**
 * Base exception for all CRUD framework errors.
 */
public class CrudException extends RuntimeException {

    private final String errorCode;

    public CrudException(String message) {
        super(message);
        this.errorCode = "CRUD_ERROR";
    }

    public CrudException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CrudException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
