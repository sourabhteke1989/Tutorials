package com.yourorg.gateway.exception;

/**
 * Thrown when JWT access token validation fails (REQ-JW-003).
 */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String message) {
        super(message);
    }

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
