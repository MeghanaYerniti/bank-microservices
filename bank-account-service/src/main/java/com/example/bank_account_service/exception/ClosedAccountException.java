package com.example.bank_account_service.exception;

public class ClosedAccountException extends RuntimeException{
    public ClosedAccountException(String message) {
        super(message);
    }
}
