// src/main/java/com/university/smartinterview/exception/ServiceUnavailableException.java
package com.university.smartinterview.exception;

/**
 * 服务不可用异常
 */
public class ServiceUnavailableException extends RuntimeException {
    private final String serviceName;
    private final int retryAfter;

    public ServiceUnavailableException(String serviceName, int retryAfter) {
        super(String.format("%s service is unavailable. Please retry after %d seconds", serviceName, retryAfter));
        this.serviceName = serviceName;
        this.retryAfter = retryAfter;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getRetryAfter() {
        return retryAfter;
    }
}