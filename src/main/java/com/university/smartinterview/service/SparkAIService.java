// src/main/java/com/university/smartinterview/service/SparkAIService.java
package com.university.smartinterview.service;

public interface SparkAIService {

    /**
     * 生成面试问题
     * @param careerDirection 职业方向
     * @return 面试问题
     */
    String generateQuestion(String careerDirection);

    /**
     * 生成面试反馈
     * @param question 面试问题
     * @param answer 用户回答
     * @return 反馈结果
     */
    String generateFeedback(String question, String answer);

    /**
     * 与星火AI进行对话
     * @param message 用户消息
     * @param context 对话上下文
     * @return AI回复
     */
    String chatWithSparkAI(String message, String context);
}