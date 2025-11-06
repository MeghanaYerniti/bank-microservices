package com.example.api_gateway.exception;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}