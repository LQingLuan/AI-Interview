// src/main/java/com/university/smartinterview/exception/AuthenticationException.java
package com.university.smartinterview.exception;

/**
 * 认证异常
 */
public class AuthenticationException extends RuntimeException {
    private final String authType;

    public AuthenticationException(String message, String authType) {
        super(message);
        this.authType = authType;
    }

    public String getAuthType() {
        return authType;
    }
}