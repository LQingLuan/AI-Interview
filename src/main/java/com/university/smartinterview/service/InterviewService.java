// src/main/java/com/university/smartinterview/service/InterviewService.java
package com.university.smartinterview.service;

import com.university.smartinterview.dto.request.FeedbackReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.dto.response.InterviewQuestionRes;
import java.util.concurrent.CompletableFuture;

public interface InterviewService {

    /**
     * 生成面试题目
     * @param careerDirection 职业方向
     * @param difficultyLevel 难度级别 (1-3)
     * @return 面试问题响应
     */
    InterviewQuestionRes generateInterviewQuestion(String careerDirection, int difficultyLevel);

    /**
     * 生成面试反馈
     *
     * @param sessionId  会话ID
     * @param answerText 用户回答文本
     * @return 反馈结果
     */
    FeedbackRes generateInterviewFeedback(String sessionId, String answerText);

    /**
     * 生成结构化面试反馈
     * @param request 反馈请求对象
     * @return 结构化反馈响应
     */
    CompletableFuture<FeedbackRes> generateStructuredFeedback(FeedbackReq request);

    /**
     * 获取面试历史记录
     * @param userId 用户ID
     * @return 面试历史列表
     */
    Object getInterviewHistory(String userId);
}