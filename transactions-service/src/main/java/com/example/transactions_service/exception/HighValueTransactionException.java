package com.example.transactions_service.exception;

public class HighValueTransactionException extends RuntimeException {
    public HighValueTransactionException(String message) {
        super(message);
    }
}
