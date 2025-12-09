// src/main/java/com/university/smartinterview/dto/response/InterviewQuestionRes.java
package com.university.smartinterview.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewQuestionRes {

    private String interviewId;  // 新增：数据库ID
    private String questionText;
    private String referenceAnswer;
    private String sessionId;    // 保持向后兼容，与interviewId相同
    // Getters and Setters
    @Getter
    private String questionId;
    @JsonProperty("question")
    private String category;
    private String difficulty;
    private String suggestedAnswer;
    private String evaluationCriteria;

    private String criteria;
    private String sampleAnswer;


    public InterviewQuestionRes() {
    }

    // 添加带参数的构造函数（可选）
    public InterviewQuestionRes(String sessionId, String questionText) {
        this.sessionId = sessionId;
        this.questionText = questionText;
    }


    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
        this.sessionId = interviewId; // 同时设置 sessionId 保持兼容
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public void setReferenceAnswer(String referenceAnswer) {this.referenceAnswer = referenceAnswer;}

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

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public String getSampleAnswer() {
        return sampleAnswer;
    }

    public void setSampleAnswer(String sampleAnswer) {
        this.sampleAnswer = sampleAnswer;
    }

}