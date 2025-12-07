// src/main/java/com/university/smartinterview/exception/InvalidParameterException.java
package com.university.smartinterview.exception;

import java.util.List;

/**
 * 参数验证异常
 */
public class InvalidParameterException extends RuntimeException {
    private final List<FieldError> fieldErrors;

    public InvalidParameterException(String message, List<FieldError> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        // Getters
        public String getField() { return field; }
        public String getMessage() { return message; }
        public Object getRejectedValue() { return rejectedValue; }
    }
}