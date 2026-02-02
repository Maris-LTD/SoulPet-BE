package com.mystictarot.backend.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, UUID id) {
        super(String.format("%s not found with id: %s", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
