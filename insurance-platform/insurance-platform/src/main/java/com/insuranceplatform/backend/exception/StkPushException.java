package com.insuranceplatform.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500 is a good status for a failed external API call
public class StkPushException extends RuntimeException {
    public StkPushException(String message) {
        super(message);
    }
}