// src/main/java/com/university/smartinterview/dto/request/FeedbackReq.java
package com.university.smartinterview.dto.request;

import jakarta.validation.constraints.NotBlank;

public class FeedbackReq {

    @NotBlank(message = "面试问题不能为空")
    private String question;

    @NotBlank(message = "用户回答不能为空")
    private String answer;

    private String context; // 可选，对话上下文

    // Getters and Setters
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}