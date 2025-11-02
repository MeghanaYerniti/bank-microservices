package com.example.bank_account_service.exception;

public class InterestMustBeZeroException extends RuntimeException {
    public InterestMustBeZeroException(String message) {
        super(message);
    }
}
