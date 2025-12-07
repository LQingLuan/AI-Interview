// src/main/java/com/university/smartinterview/exception/AIException.java
package com.university.smartinterview.exception;

/**
 * AI服务异常基类
 */
public class AIException extends RuntimeException {
    private final String errorCode;
    private final String sessionId;

    public AIException(String errorCode, String message, String sessionId) {
        super(message);
        this.errorCode = errorCode;
        this.sessionId = sessionId;
    }

    public AIException(String errorCode, String message, String sessionId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.sessionId = sessionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getSessionId() {
        return sessionId;
    }
}