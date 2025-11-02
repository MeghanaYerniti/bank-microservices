package com.example.bank_account_service.exception;

public class RequiresPanException extends RuntimeException {
    public RequiresPanException(String message) {
        super(message);
    }
}