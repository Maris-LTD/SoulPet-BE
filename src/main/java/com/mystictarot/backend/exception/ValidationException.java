package com.mystictarot.backend.exception;

/**
 * Exception thrown when request validation fails
 * This exception should be mapped to HTTP 400 BAD_REQUEST
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
