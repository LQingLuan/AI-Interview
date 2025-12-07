// src/main/java/com/university/smartinterview/service/impl/SparkAIServiceImpl.java
package com.university.smartinterview.service.impl;

import com.university.smartinterview.config.IflytekConfig;
import com.university.smartinterview.service.SparkAIService;
import com.university.smartinterview.utils.SparkAIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SparkAIServiceImpl implements SparkAIService {

    private final SparkAIClient sparkAIClient;
    private final IflytekConfig.SparkAIConfig sparkAIConfig;

    @Autowired
    public SparkAIServiceImpl(SparkAIClient sparkAIClient,
                              IflytekConfig.SparkAIConfig sparkAIConfig) {
        this.sparkAIClient = sparkAIClient;
        this.sparkAIConfig = sparkAIConfig;
    }

    @Override
    public String generateQuestion(String careerDirection) {
        String prompt = String.format(
                "作为%s领域的面试官，请生成一个专业的技术面试问题，要求考察候选人的专业深度和解决问题的能力",
                careerDirection
        );
        return sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);
    }

    @Override
    public String generateFeedback(String question, String answer) {
        String prompt = String.format(
                "请根据以下面试问题和候选人的回答，提供专业的反馈：\n问题：%s\n回答：%s",
                question, answer
        );
        return sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);
    }

    @Override
    public String chatWithSparkAI(String message, String context) {
        return sparkAIClient.callSparkAI(message, context, sparkAIConfig);
    }
}