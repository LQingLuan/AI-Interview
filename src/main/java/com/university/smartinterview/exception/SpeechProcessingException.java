// src/main/java/com/university/smartinterview/exception/SpeechProcessingException.java
package com.university.smartinterview.exception;

/**
 * 语音处理异常
 */
public class SpeechProcessingException extends AIException {
    private final String audioInfo;

    public SpeechProcessingException(String errorCode, String message, String sessionId, String audioInfo) {
        super(errorCode, message, sessionId);
        this.audioInfo = audioInfo;
    }

    public SpeechProcessingException(String errorCode, String message, String sessionId, String audioInfo, Throwable cause) {
        super(errorCode, message, sessionId, cause);
        this.audioInfo = audioInfo;
    }

    public String getAudioInfo() {
        return audioInfo;
    }
}