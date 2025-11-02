package com.example.transactions_service.exception;

public class MinimumBalanceException extends RuntimeException {
    public MinimumBalanceException(String message) {
        super(message);
    }
}
