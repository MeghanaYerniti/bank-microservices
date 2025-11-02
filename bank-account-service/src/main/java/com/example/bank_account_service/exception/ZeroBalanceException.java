package com.example.bank_account_service.exception;

public class ZeroBalanceException extends RuntimeException {
    public ZeroBalanceException(String message) {
        super(message);
    }
}
