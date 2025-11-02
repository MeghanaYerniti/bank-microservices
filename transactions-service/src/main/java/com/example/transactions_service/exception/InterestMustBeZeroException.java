package com.example.transactions_service.exception;

public class InterestMustBeZeroException extends RuntimeException {
    public InterestMustBeZeroException(String message) {
        super(message);
    }
}
