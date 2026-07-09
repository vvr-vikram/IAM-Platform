package com.enterprise.iam.exception;

import org.springframework.http.HttpStatus;

public class IamException extends RuntimeException {
    private final HttpStatus status;

    public IamException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
