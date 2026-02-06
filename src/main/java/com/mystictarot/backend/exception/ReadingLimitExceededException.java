package com.mystictarot.backend.exception;

/**
 * Thrown when user has exceeded their weekly reading limit for their plan
 */
public class ReadingLimitExceededException extends RuntimeException {

    public ReadingLimitExceededException(String message) {
        super(message);
    }
}
