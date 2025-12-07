// src/main/java/com/university/smartinterview/exception/AIServiceException.java
package com.university.smartinterview.exception;

/**
 * 讯飞AI服务异常
 */
public class AIServiceException extends AIException {
    public AIServiceException(String errorCode, String message, String sessionId) {
        super(errorCode, message, sessionId);
    }

    public AIServiceException(String errorCode, String message, String sessionId, Throwable cause) {
        super(errorCode, message, sessionId, cause);
    }
}