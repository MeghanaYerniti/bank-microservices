package com.example.customer_service.exception;

public class ZeroBalanceException extends RuntimeException {
    public ZeroBalanceException(String message) {
        super(message);
    }
}
