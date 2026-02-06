package com.mystictarot.backend.exception;

/**
 * Thrown when Gemini AI API call fails after retries (timeout, 5xx, etc.)
 */
public class GeminiServiceException extends RuntimeException {

    public GeminiServiceException(String message) {
        super(message);
    }

    public GeminiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
