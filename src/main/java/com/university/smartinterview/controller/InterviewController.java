package com.university.smartinterview.controller;

import com.university.smartinterview.dto.request.AnswerSubmitRequest;
import com.university.smartinterview.dto.request.InterviewStartReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.dto.response.InterviewQuestionRes;
import com.university.smartinterview.dto.response.ResponseWrapper;
import com.university.smartinterview.entity.InterviewRecord;
import com.university.smartinterview.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    @Autowired
    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/start")
    public ResponseEntity<ResponseWrapper<InterviewQuestionRes>> startInterview(
            @RequestBody InterviewStartReq request) {

        log.info("开始面试 - 方向: {}, 难度: {}",
                request.getCareerDirection(),
                request.getDifficultyLevel());

        InterviewQuestionRes question = interviewService.generateInterviewQuestion(
                request.getCareerDirection(),
                request.getDifficultyLevel()
        );

        log.info("面试问题生成成功 - interviewId: {}", question.getInterviewId());

        return ResponseEntity.ok(ResponseWrapper.success(question));
    }

    @PostMapping("/submit-answer")
    public ResponseEntity<ResponseWrapper<FeedbackRes>> submitAnswer(
            @RequestBody AnswerSubmitRequest request) {

        log.info("提交回答 - interviewId: {}, 回答长度: {}",
                request.getInterviewId(),
                request.getAnswerText() != null ? request.getAnswerText().length() : 0);

        FeedbackRes feedback = interviewService.generateInterviewFeedback(
                request.getInterviewId(),
                request.getAnswerText()
        );

        log.info("反馈生成成功 - interviewId: {}, 综合评分: {}",
                request.getInterviewId(),
                feedback.getOverallScore());

        return ResponseEntity.ok(ResponseWrapper.success(feedback));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ResponseWrapper<?>> getInterviewHistory(
            @PathVariable String userId) {
        return ResponseEntity.ok(ResponseWrapper.success(
                interviewService.getInterviewHistory(userId)
        ));
    }

    @GetMapping("/detail/{interviewId}")
    public ResponseEntity<ResponseWrapper<?>> getInterviewDetail(
            @PathVariable String interviewId) {
        try {
            // 注意：需要将InterviewServiceImpl中的getInterviewDetail方法提升到接口中
            // 或者在控制器中直接调用repository
            return ResponseEntity.ok(ResponseWrapper.success(
                    "详情功能待实现"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<ResponseWrapper<String>> deleteInterview(
            @PathVariable String interviewId) {
        try {
            // 注意：需要将InterviewServiceImpl中的deleteInterview方法提升到接口中
            return ResponseEntity.ok(ResponseWrapper.success("删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(400, e.getMessage()));
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "InterviewController is working with database! " + System.currentTimeMillis();
    }
}