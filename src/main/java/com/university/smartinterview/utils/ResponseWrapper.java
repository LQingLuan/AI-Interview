// src/main/java/com/university/smartinterview/utils/ResponseWrapper.java
package com.university.smartinterview.utils;

/**
 * 统一响应封装工具类
 */
public class ResponseWrapper<T> {
    private int code;
    private String message;
    private T data;

    private ResponseWrapper(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(200, "success", data);
    }

    public static <T> ResponseWrapper<T> success() {
        return new ResponseWrapper<>(200, "success", null);
    }

    public static <T> ResponseWrapper<T> error(int code, String message) {
        return new ResponseWrapper<>(code, message, null);
    }

    public static <T> ResponseWrapper<T> of(int code, String message, T data) {
        return new ResponseWrapper<>(code, message, data);
    }

    // Getters
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}