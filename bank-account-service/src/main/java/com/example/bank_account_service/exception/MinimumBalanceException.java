package com.example.bank_account_service.exception;

public class MinimumBalanceException extends RuntimeException {
    public MinimumBalanceException(String message) {
        super(message);
    }
}
