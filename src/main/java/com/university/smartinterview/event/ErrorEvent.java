// src/main/java/com/university/smartinterview/event/ErrorEvent.java
package com.university.smartinterview.event;

public class ErrorEvent {
    private final String sessionId;
    private final String errorCode;
    private final String errorMessage;

    public ErrorEvent(String sessionId, String errorCode, String errorMessage) {
        this.sessionId = sessionId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}