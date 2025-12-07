// src/main/java/com/university/smartinterview/controller/InterviewController.java
package com.university.smartinterview.controller;

import com.university.smartinterview.dto.AnswerSubmitRequest;
import com.university.smartinterview.dto.request.InterviewStartReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.dto.response.InterviewQuestionRes;
import com.university.smartinterview.dto.response.ResponseWrapper;
import com.university.smartinterview.service.InterviewService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/")
public class InterviewController {
    public static final Logger logger = LoggerFactory.getLogger(InterviewController.class);

    // 添加构造后初始化方法
    @PostConstruct
    public void init() {
        logger.info(">>>> InterviewController 已初始化 <<<<");
    }

    private final InterviewService interviewService;

    @Autowired
    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /**
     * 启动面试流程
     * @param request 包含职业方向等面试参数
     * @return 面试题目
     */
    @PostMapping("/start")
    public ResponseEntity<ResponseWrapper<InterviewQuestionRes>> startInterview(
            @RequestBody InterviewStartReq request) {
        String sessionId = UUID.randomUUID().toString();

        InterviewQuestionRes question = interviewService.generateInterviewQuestion(
                request.getCareerDirection(),
                request.getDifficultyLevel()
        );
        question.setSessionId(sessionId);

        logger.info("生成面试问题 - 方向: {}, 难度: {}, 问题: {}",
                request.getCareerDirection(),
                request.getDifficultyLevel(),
                question.getQuestionText());

        return ResponseEntity.ok(ResponseWrapper.success(question));
    }

    /**
     * 提交用户回答并获取反馈
     * @param request 包含会话ID和回答内容
     * @return 结构化的面试反馈
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<ResponseWrapper<FeedbackRes>> submitAnswer(
            @RequestBody AnswerSubmitRequest request) {

        logger.info("提交回答 - sessionId: {}, 回答长度: {}",
                request.getSessionId(),
                request.getAnswerText().length());

        FeedbackRes feedback = interviewService.generateInterviewFeedback(
                request.getSessionId(),
                request.getAnswerText()
        );

        logger.info("反馈生成成功 - sessionId: {}, 综合评分: {}",
                request.getSessionId(),
                feedback.getOverallScore());

        return ResponseEntity.ok(ResponseWrapper.success(feedback));
    }

    /**
     * 获取面试历史记录
     * @param userId 用户ID
     * @return 用户的历史面试记录
     */
    @GetMapping("/history")
    public ResponseEntity<ResponseWrapper<?>> getInterviewHistory(
            @RequestParam String userId) {
        return ResponseEntity.ok(ResponseWrapper.success(
                interviewService.getInterviewHistory(userId)
        ));
    }

    @GetMapping("/ping")
    public String ping() {
        return "InterviewController is working! " + System.currentTimeMillis();
    }
}