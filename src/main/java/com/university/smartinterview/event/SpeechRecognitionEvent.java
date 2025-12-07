// src/main/java/com/university/smartinterview/event/SpeechRecognitionEvent.java
package com.university.smartinterview.event;

import com.university.smartinterview.dto.response.SpeechRecognitionRes;

public class SpeechRecognitionEvent {
    private final String sessionId;
    private final SpeechRecognitionRes result;

    public SpeechRecognitionEvent(String sessionId, SpeechRecognitionRes result) {
        this.sessionId = sessionId;
        this.result = result;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SpeechRecognitionRes getResult() {
        return result;
    }
}