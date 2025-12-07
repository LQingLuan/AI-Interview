// src/main/java/com/university/smartinterview/service/impl/InterviewServiceImpl.java
package com.university.smartinterview.service.impl;

import com.university.smartinterview.config.IflytekConfig;
import com.university.smartinterview.dto.request.FeedbackReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.dto.response.InterviewQuestionRes;
import com.university.smartinterview.service.InterviewService;
import com.university.smartinterview.service.SparkAIService;
import com.university.smartinterview.utils.SparkAIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.stereotype.Service;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.university.smartinterview.controller.InterviewController.logger;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final SparkAIService sparkAIService;
    private final SparkAIClient sparkAIClient;
    private final IflytekConfig.SparkAIConfig sparkAIConfig;



    // 使用ConcurrentHashMap缓存sessionId与问题的映射
    private final Map<String, String> questionCache = new ConcurrentHashMap<>();

    @Autowired
    public InterviewServiceImpl(SparkAIService sparkAIService,
                                SparkAIClient sparkAIClient,
                                IflytekConfig.SparkAIConfig sparkAIConfig) {
        this.sparkAIService = sparkAIService;
        this.sparkAIClient = sparkAIClient;
        this.sparkAIConfig = sparkAIConfig;
    }

    @Override
    public InterviewQuestionRes generateInterviewQuestion(String careerDirection, int difficultyLevel) {
        // 生成唯一的会话ID
        String sessionId = UUID.randomUUID().toString();

        // 构建提示词
        String prompt = String.format(
                "你是一个%s领域的资深面试官，请生成一个难度级别为%d的面试问题。要求：\n" +
                        "1. 问题应考察候选人的专业知识和解决问题的能力\n" +
                        "2. 提供问题的分类\n" +
                        "3. 提供评估标准\n" +
                        "4. 提供参考答案（简明扼要）\n" +
                        "输出格式为JSON：{\"question\":\"问题内容\",\"category\":\"问题分类\",\"difficulty\":\"难度描述\",\"criteria\":\"评估标准\",\"answer\":\"参考答案\"}",
                careerDirection, difficultyLevel
        );

        // 调用讯飞星火API生成问题
        String response = sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);

        // 缓存问题与会话ID的关联
        questionCache.put(sessionId, response.trim());

        // 解析响应并构建返回对象
        InterviewQuestionRes questionRes = new InterviewQuestionRes();
        questionRes.setQuestionText(response.trim());
        return questionRes;
    }

    @Override
    public FeedbackRes generateInterviewFeedback(String sessionId, String answerText) {
        // 从缓存中获取对应的问题
        String question = questionCache.get(sessionId);

        if (question == null) {
            logger.error("找不到sessionId对应的问题: {}", sessionId);
            throw new RuntimeException("找不到对应的问题");
        }

        // 构建反馈提示词
        String prompt = String.format(
                "你是一个资深面试官，请根据以下面试问题和候选人的回答，提供结构化的反馈：\n" +
                        "面试问题：%s\n" +
                        "候选人回答：%s\n\n" +
                        "反馈要求：\n" +
                        "1. overallScore: 综合评分（1-10分）\n" +
                        "2. overallFeedback: 总体评价\n" +
                        "3. dimensions: [{\"name\":\"维度名称\", \"score\":维度评分, \"evaluation\":\"维度评价\"}] 维度包括：专业知识、问题解决、沟通表达、技术深度、逻辑思维\n" +
                        "4. improvementSuggestions: [\"建议1\", \"建议2\"]\n" +
                        "输出格式必须是JSON，不要有任何其他文本",
                question, answerText
        );

        // 调用讯飞星火API生成结构化反馈
        String jsonResponse = sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);
        logger.info("AI返回的JSON反馈: {}", jsonResponse);

        // 解析JSON响应并映射到FeedbackRes对象
        try {
            JSONObject json = new JSONObject(jsonResponse);
            FeedbackRes feedbackRes = new FeedbackRes();

            // 设置综合评分
            feedbackRes.setOverallScore(json.getDouble("overallScore") + "/10");

            // 设置总体反馈
            feedbackRes.setOverallFeedback(json.getString("overallFeedback"));

            // 设置维度评价
            JSONArray dimensionsArray = json.getJSONArray("dimensions");
            List<FeedbackRes.DimensionEvaluation> dimensions = new ArrayList<>();
            for (int i = 0; i < dimensionsArray.length(); i++) {
                JSONObject dimObj = dimensionsArray.getJSONObject(i);
                FeedbackRes.DimensionEvaluation dim = new FeedbackRes.DimensionEvaluation();
                dim.setDimensionName(dimObj.getString("name"));
                dim.setScore(String.valueOf(dimObj.getDouble("score")));
                dim.setEvaluation(dimObj.getString("evaluation"));
                dimensions.add(dim);
            }
            feedbackRes.setDimensions(dimensions);

            // 设置改进建议
            JSONArray suggestionsArray = json.getJSONArray("improvementSuggestions");
            List<String> suggestions = new ArrayList<>();
            for (int i = 0; i < suggestionsArray.length(); i++) {
                suggestions.add(suggestionsArray.getString(i));
            }
            feedbackRes.setImprovementSuggestions(suggestions);

            // 反馈生成后清理缓存
            //questionCache.remove(sessionId);

            return feedbackRes;
        } catch (Exception e) {
            logger.error("解析反馈JSON失败", e);
            throw new RuntimeException("解析反馈数据失败: " + e.getMessage(), e);
        }
    }

    // 添加缓存清理端点
    @PostMapping("/clear-cache/{sessionId}")
    public ResponseEntity<String> clearCache(@PathVariable String sessionId) {
        questionCache.remove(sessionId);
        return ResponseEntity.ok("缓存已清除");
    }


    @Override
    @Async("asyncFeedbackExecutor")
    public CompletableFuture<FeedbackRes> generateStructuredFeedback(FeedbackReq request) {
        // 构建结构化反馈提示词
        String prompt = String.format(
                "你是一个资深面试官，请根据以下面试问题和候选人的回答，提供结构化的反馈：\n" +
                        "面试问题：%s\n" +
                        "候选人回答：%s\n\n" +
                        "反馈要求（输出为JSON格式）：\n" +
                        "1. overallScore: 综合评分（1-10分）\n" +
                        "2. overallFeedback: 总体评价\n" +
                        "3. dimensions: [{\"name\":\"维度名称\", \"score\":\"维度评分\", \"evaluation\":\"维度评价\"}] 维度包括：技术能力、沟通表达、问题解决、专业知识\n" +
                        "4. improvementSuggestions: [\"建议1\", \"建议2\"]\n" +
                        "5. sampleAnswer: 参考答案",
                request.getQuestion(), request.getAnswer()
        );

        // 调用讯飞星火API生成结构化反馈
        String jsonResponse = sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);

        // 解析JSON响应并映射到FeedbackRes对象
        FeedbackRes feedbackRes = new FeedbackRes();
        try {
            JSONObject json = new JSONObject(jsonResponse);

            // 设置综合评分
            feedbackRes.setOverallScore(json.getDouble("overallScore") + "/10");

            // 设置总体反馈
            feedbackRes.setOverallFeedback(json.getString("overallFeedback"));

            // 设置维度评价
            JSONArray dimensionsArray = json.getJSONArray("dimensions");
            List<FeedbackRes.DimensionEvaluation> dimensions = new ArrayList<>();
            for (int i = 0; i < dimensionsArray.length(); i++) {
                JSONObject dimObj = dimensionsArray.getJSONObject(i);
                FeedbackRes.DimensionEvaluation dim = new FeedbackRes.DimensionEvaluation();
                dim.setDimensionName(dimObj.getString("name"));
                dim.setScore(dimObj.getString("score"));
                dim.setEvaluation(dimObj.getString("evaluation"));
                dimensions.add(dim);
            }
            feedbackRes.setDimensions(dimensions);

            // 设置改进建议
            JSONArray suggestionsArray = json.getJSONArray("improvementSuggestions");
            List<String> suggestions = new ArrayList<>();
            for (int i = 0; i < suggestionsArray.length(); i++) {
                suggestions.add(suggestionsArray.getString(i));
            }
            feedbackRes.setImprovementSuggestions(suggestions);

            // 设置参考答案
            feedbackRes.setSampleAnswer(json.getString("sampleAnswer"));

        } catch (Exception e) {
            logger.error("解析结构化反馈JSON失败", e);
        }

        return CompletableFuture.completedFuture(feedbackRes);
    }

    @Override
    public Object getInterviewHistory(String userId) {
        // TODO: 从数据库查询用户的面试历史记录
        return null;
    }
}