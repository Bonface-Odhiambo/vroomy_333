package com.insuranceplatform.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request is appropriate here
public class InvalidIraNumberException extends RuntimeException {
    public InvalidIraNumberException(String message) {
        super(message);
    }
}