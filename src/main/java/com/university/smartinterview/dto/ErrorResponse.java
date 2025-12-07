// src/main/java/com/university/smartinterview/dto/ErrorResponse.java
package com.university.smartinterview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 错误响应对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private String message;
    private String field;
    private Object rejectedValue;
    private String sessionId;
    private String details;
    private String retryAfter;
    private String authType;

    // 简单错误
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // 带字段信息的错误
    public ErrorResponse(String code, String message, String field, Object rejectedValue) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    // AI服务错误
    public ErrorResponse(String code, String message, String sessionId) {
        this.code = code;
        this.message = message;
        this.sessionId = sessionId;
    }

    // 服务不可用错误
    public ErrorResponse(String code, String message, String field, int retryAfter) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.retryAfter = String.valueOf(retryAfter);
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public Object getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getRetryAfter() { return retryAfter; }
    public void setRetryAfter(String retryAfter) { this.retryAfter = retryAfter; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
}