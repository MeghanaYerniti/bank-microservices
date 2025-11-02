package com.example.customer_service.exception;

public class RequiresPanException extends RuntimeException {
    public RequiresPanException(String message) {
        super(message);
    }
}