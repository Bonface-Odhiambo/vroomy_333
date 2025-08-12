package com.insuranceplatform.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructor with a simple message.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * NEW CONSTRUCTOR: Allows wrapping another exception.
     * This is what we need to fix the error in AgentService.
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}