package com.example.customer_service.exception;

public class ClosedAccountException extends RuntimeException{
    public ClosedAccountException(String message) {
        super(message);
    }
}
