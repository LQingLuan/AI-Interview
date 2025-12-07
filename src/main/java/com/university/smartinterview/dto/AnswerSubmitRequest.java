package com.university.smartinterview.dto;

public class AnswerSubmitRequest {
    private String sessionId;
    private String answerText;

    // getters/setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}