// src/main/java/com/university/smartinterview/dto/IflytekDTO.java
package com.university.smartinterview.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

public class IflytekDTO {

    // 讯飞星火请求对象
    @Data
    public static class SparkAIRequest {
        private String question;
        private String context;
    }

    // 讯飞配置响应对象
    @Data
    @AllArgsConstructor
    public static class SparkAIConfigResponse {
        private String apiUrl;
        private String appId;
    }

    // 讯飞语音识别参数
    @Data
    public static class SpeechRecognitionParams {
        private String sessionId;
        private String audioFormat;
        private Integer sampleRate;
    }

    // 讯飞API错误响应
    @Data
    @AllArgsConstructor
    public static class IflytekErrorResponse {
        private Integer code;
        private String message;
        private String sid; // 会话ID
    }

    // 面试历史记录
    @Data
    public static class InterviewHistory {
        private String sessionId;
        private String timestamp;
        private String careerDirection;
        private String difficulty;
        private String overallScore;
        private String questionSummary;
    }
}