/*
package com.university.smartinterview.controller;

import com.university.smartinterview.config.IflytekConfig;
import com.university.smartinterview.dto.IflytekDTO;
import com.university.smartinterview.service.SparkAIService;
import com.university.smartinterview.utils.SparkAIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 讯飞星火API代理控制器
 * 提供与讯飞星火API的直接交互端点

@RestController
@RequestMapping("/iflytek")
public class IflytekController {

    private final SparkAIService sparkAIService;
    private final SparkAIClient sparkAIClient;
    private final IflytekConfig.SparkAIConfig sparkAIConfig;

    @Autowired
    public IflytekController(SparkAIService sparkAIService,
                             SparkAIClient sparkAIClient,
                             IflytekConfig.SparkAIConfig sparkAIConfig) {
        this.sparkAIService = sparkAIService;
        this.sparkAIClient = sparkAIClient;
        this.sparkAIConfig = sparkAIConfig;
    }

    /**
     * 直接调用讯飞星火API
     * @param request 包含问题和上下文
     * @return 星火API的原始响应

    @PostMapping("/direct-call")
    public ResponseEntity<String> directCallToSparkAI(@RequestBody IflytekDTO.SparkAIRequest request) {
        String response = sparkAIClient.callSparkAI(
                request.getQuestion(),
                request.getContext(),
                sparkAIConfig
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 获取讯飞API配置（前端可能需要这些配置建立直接连接）
     * @return 安全的配置信息（不含密钥）

    @GetMapping("/config")
    public ResponseEntity<IflytekDTO.SparkAIConfigResponse> getSparkAIConfig() {
        IflytekDTO.SparkAIConfigResponse config = new IflytekDTO.SparkAIConfigResponse(
                sparkAIConfig.getApiUrl(),
                sparkAIConfig.getAppId()
                // 注意：不返回敏感信息如apiKey和apiSecret
        );
        return ResponseEntity.ok(config);
    }

    /**
     * 生成面试题目（直接调用）
     * @param careerDirection 职业方向
     * @return 面试题目

    @GetMapping("/generate-question")
    public ResponseEntity<String> generateQuestionDirectly(
            @RequestParam String careerDirection) {
        return ResponseEntity.ok(
                sparkAIService.generateQuestion(careerDirection)
        );
    }

    /**
     * 生成面试反馈（直接调用）
     * @param question 面试问题
     * @param answer 用户回答
     * @return 反馈和建议

    @PostMapping("/generate-feedback")
    public ResponseEntity<String> generateFeedbackDirectly(
            @RequestParam String question,
            @RequestParam String answer) {
        return ResponseEntity.ok(
                sparkAIService.generateFeedback(question, answer)
        );
    }
}
*/
