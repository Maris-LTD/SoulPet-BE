package com.mystictarot.backend.exception;

/**
 * Thrown when user has insufficient credits for follow-up question
 */
public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException(String message) {
        super(message);
    }
}
