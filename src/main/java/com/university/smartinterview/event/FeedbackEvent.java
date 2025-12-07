// src/main/java/com/university/smartinterview/event/FeedbackEvent.java
package com.university.smartinterview.event;

import com.university.smartinterview.dto.response.FeedbackRes;

public class FeedbackEvent {
    private final String sessionId;
    private final FeedbackRes feedback;

    public FeedbackEvent(String sessionId, FeedbackRes feedback) {
        this.sessionId = sessionId;
        this.feedback = feedback;
    }

    public String getSessionId() {
        return sessionId;
    }

    public FeedbackRes getFeedback() {
        return feedback;
    }
}