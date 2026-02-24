package com.framework.crud.exception;

/**
 * Thrown when the current user lacks the required permission for an operation.
 */
public class CrudAccessDeniedException extends CrudException {

    public CrudAccessDeniedException(String entityType, String operation, String requiredPermission) {
        super("ACCESS_DENIED",
                String.format("Access denied: operation '%s' on entity '%s' requires permission '%s'",
                        operation, entityType, requiredPermission));
    }

    public CrudAccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }
}
