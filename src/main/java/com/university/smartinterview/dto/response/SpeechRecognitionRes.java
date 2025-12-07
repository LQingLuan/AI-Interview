// src/main/java/com/university/smartinterview/dto/response/SpeechRecognitionRes.java
package com.university.smartinterview.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SpeechRecognitionRes {

    private String partialResult;
    private String finalResult;
    public Boolean isFinal;
    private Integer progress;
    private String sessionId;
    private Integer status; // 0-成功, 1-处理中, 2-失败

    // Getters and Setters
    public String getPartialResult() {
        return partialResult;
    }

    public void setPartialResult(String partialResult) {
        this.partialResult = partialResult;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public void setIsFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public SpeechRecognitionRes get() {
        return null;
    }
}

