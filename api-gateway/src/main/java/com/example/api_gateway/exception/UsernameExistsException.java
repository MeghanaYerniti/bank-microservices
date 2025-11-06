package com.example.api_gateway.exception;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException(String message) { super(message); }
}