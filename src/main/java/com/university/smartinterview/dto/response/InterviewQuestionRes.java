// src/main/java/com/university/smartinterview/dto/response/InterviewQuestionRes.java
package com.university.smartinterview.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewQuestionRes {

    private String questionId;
    @JsonProperty("question")
    private String questionText;
    private String category;
    private String difficulty;
    private String suggestedAnswer;
    private String evaluationCriteria;
    private String sessionId;



    public InterviewQuestionRes() {
    }

    // 添加带参数的构造函数（可选）
    public InterviewQuestionRes(String sessionId, String questionText) {
        this.sessionId = sessionId;
        this.questionText = questionText;
    }


    // Getters and Setters
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getSuggestedAnswer() {
        return suggestedAnswer;
    }

    public void setSuggestedAnswer(String suggestedAnswer) {
        this.suggestedAnswer = suggestedAnswer;
    }

    public String getEvaluationCriteria() {
        return evaluationCriteria;
    }

    public void setEvaluationCriteria(String evaluationCriteria) {
        this.evaluationCriteria = evaluationCriteria;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}