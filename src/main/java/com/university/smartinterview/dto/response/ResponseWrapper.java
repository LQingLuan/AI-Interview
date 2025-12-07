package com.university.smartinterview.dto.response;

public class ResponseWrapper<T> {

    private int code;
    private String message;
    private T data;

    // 私有构造函数
    private ResponseWrapper(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功响应
    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(200, "success", data);
    }

    public static <T> ResponseWrapper<T> success() {
        return new ResponseWrapper<>(200, "success", null);
    }

    // 错误响应（新增：带错误数据的响应）
    public static <T> ResponseWrapper<T> error(T errorData) {
        return new ResponseWrapper<>(500, "error", errorData);
    }

    // 错误响应（带状态码和消息）
    public static <T> ResponseWrapper<T> error(int code, String message) {
        return new ResponseWrapper<>(code, message, null);
    }

    // 错误响应（带状态码、消息和错误数据）
    public static <T> ResponseWrapper<T> error(int code, String message, T errorData) {
        return new ResponseWrapper<>(code, message, errorData);
    }

    // 自定义响应
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