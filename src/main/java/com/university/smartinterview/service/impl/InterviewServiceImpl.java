package com.university.smartinterview.service.impl;

import com.university.smartinterview.config.IflytekConfig;
import com.university.smartinterview.dto.request.FeedbackReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.dto.response.InterviewQuestionRes;
import com.university.smartinterview.entity.FeedbackDimension;
import com.university.smartinterview.entity.InterviewRecord;
import com.university.smartinterview.repository.FeedbackDimensionRepository;
import com.university.smartinterview.repository.InterviewRecordRepository;
import com.university.smartinterview.service.InterviewService;
import com.university.smartinterview.service.SparkAIService;
import com.university.smartinterview.utils.SparkAIClient;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRecordRepository interviewRecordRepository;
    private final FeedbackDimensionRepository feedbackDimensionRepository;
    private final SparkAIService sparkAIService;
    private final SparkAIClient sparkAIClient;
    private final IflytekConfig.SparkAIConfig sparkAIConfig;

    @Autowired
    public InterviewServiceImpl(InterviewRecordRepository interviewRecordRepository,
                                FeedbackDimensionRepository feedbackDimensionRepository,
                                SparkAIService sparkAIService,
                                SparkAIClient sparkAIClient,
                                IflytekConfig.SparkAIConfig sparkAIConfig) {
        this.interviewRecordRepository = interviewRecordRepository;
        this.feedbackDimensionRepository = feedbackDimensionRepository;
        this.sparkAIService = sparkAIService;
        this.sparkAIClient = sparkAIClient;
        this.sparkAIConfig = sparkAIConfig;
    }

    @Override
    public InterviewQuestionRes generateInterviewQuestion(String careerDirection, int difficultyLevel) {
        // 生成唯一的面试ID
        String interviewId = "INT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 构建提示词
        String prompt = String.format(
                "你是一个%s领域的资深面试官，请生成一个难度级别为%d的面试问题。\n" +
                        "请以JSON格式输出，并且只输出JSON，不要有任何其他文本。\n" +
                        "JSON格式如下：\n" +
                        "{\n" +
                        "  \"question\": \"问题内容\",\n" +
                        "  \"category\": \"问题分类\",\n" +
                        "  \"difficulty\": \"难度描述，如'难度级别2'或'中级'\",\n" +
                        "  \"criteria\": \"评估标准，描述候选人需要展示哪些能力\",\n" +
                        "  \"answer\": \"简明扼要的参考答案\"\n" +
                        "}\n" +
                        "具体要求：\n" +
                        "1. 问题应考察候选人的专业知识和解决问题的能力\n" +
                        "2. 问题分类要准确反映技术领域\n" +
                        "3. 难度描述要符合%d的级别\n" +
                        "4. 评估标准要具体、可衡量\n" +
                        "5. 参考答案要专业、准确、简洁",
                careerDirection, difficultyLevel,difficultyLevel
        );

        // 调用讯飞星火API生成问题
        String aiResponse = sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);

        // 解析AI返回的问题（可能包含JSON或纯文本）
        Map<String, String> extractedData = extractQuestionAndAnswerFromAIResponse(aiResponse);
        String questionText = extractedData.get("question");
        String referenceAnswer = extractedData.get("answer");

        // 保存到数据库
        InterviewRecord record = new InterviewRecord();
        record.setInterviewId(interviewId);
        record.setCareerDirection(careerDirection);
        record.setDifficultyLevel(difficultyLevel);
        record.setQuestionText(questionText);
        record.setReferenceAnswer(referenceAnswer);
        record.setStatus(InterviewRecord.InterviewStatus.PENDING);

        interviewRecordRepository.save(record);
        log.info("面试问题已保存到数据库，interviewId: {}", interviewId);

        // 构建返回对象
        InterviewQuestionRes questionRes = new InterviewQuestionRes();
        questionRes.setInterviewId(interviewId);
        questionRes.setQuestionText(questionText);
        questionRes.setReferenceAnswer(referenceAnswer);
        questionRes.setSessionId(interviewId); // 保持向后兼容
        return questionRes;

    }

    @Override
    public FeedbackRes generateInterviewFeedback(String interviewId, String answerText) {
        log.info("开始生成反馈，interviewId: {}, 回答长度: {}", interviewId, answerText.length());

        // 1. 查询面试记录
        InterviewRecord record = interviewRecordRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new RuntimeException("未找到面试记录: " + interviewId));

        log.info("数据库中已有的参考答案: {}",
                record.getReferenceAnswer() != null ? record.getReferenceAnswer().substring(0, Math.min(100, record.getReferenceAnswer().length())) : "无");

        // 2. 更新回答内容
        record.setAnswerText(answerText);
        record.setStatus(InterviewRecord.InterviewStatus.ANSWERED);
        interviewRecordRepository.save(record);

        // 3. 构建反馈提示词
        String prompt = String.format(
                "你是一个资深面试官，请根据以下面试问题和候选人的回答，提供结构化的反馈：\n" +
                        "面试问题：%s\n" +
                        "候选人回答：%s\n\n" +
                        "请严格按照以下要求生成反馈：\n" +
                        "1. 综合评分：根据回答质量给出0-10分的分数\n" +
                        "2. 总体评价：简要总结候选人的表现\n" +
                        "3. 各维度评价：针对每个维度，根据候选人的具体回答给出评分和详细评价\n" +
                        "4. 改进建议：提供2-3条具体的改进建议\n\n" +
                        "请以JSON格式输出，并且只输出JSON，不要有任何其他文本。\n" +
                        "JSON格式如下：\n" +
                        "{\n" +
                        "  \"overallScore\": \"8.6\",\n" +
                        "  \"overallFeedback\": \"候选人的回答...\",\n" +
                        "  \"dimensions\": [\n" +
                        "    {\"dimensionName\": \"专业知识\", \"score\": \"8.0\", \"evaluation\": \"根据候选人对...的理解，具体评价...\"},\n" +
                        "    {\"dimensionName\": \"问题解决\", \"score\": \"7.5\", \"evaluation\": \"候选人在解决问题方面...\"},\n" +
                        "    {\"dimensionName\": \"沟通表达\", \"score\": \"8.4\", \"evaluation\": \"候选人的表达清晰...\"},\n" +
                        "    {\"dimensionName\": \"技术深度\", \"score\": \"7.2\", \"evaluation\": \"对技术细节的掌握...\"},\n" +
                        "    {\"dimensionName\": \"逻辑思维\", \"score\": \"8.8\", \"evaluation\": \"思维逻辑...\"}\n" +
                        "  ],\n" +
                        "  \"improvementSuggestions\": [\"建议1\", \"建议2\", \"建议3\"]\n" +
                        "}\n" +
                        "重要提示：\n" +
                        "1. 必须根据候选人的实际回答内容生成具体的评价，不能使用通用模板\n" +
                        "2. 如果回答不相关或太简短，评分不能超过3分\n" +
                        "3. 评价内容要具体，指出回答中的优点和不足\n" +
                        "4. 改进建议要针对回答中的具体问题提出",
                record.getQuestionText(), answerText
        );

        try {
            // 4. 调用讯飞星火API生成结构化反馈
            String jsonResponse = sparkAIClient.callSparkAI(prompt, "", sparkAIConfig);
            log.info("AI返回的JSON反馈，长度: {}", jsonResponse.length());

            // 5. 解析并保存反馈数据
            FeedbackRes feedbackRes = parseAndSaveFeedback(record, jsonResponse);
            log.info("反馈生成成功，interviewId: {}, 综合评分: {}", interviewId, feedbackRes.getOverallScore());

            // 确保参考答案被包含在返回结果中
            if ( record.getReferenceAnswer() != null) {
                feedbackRes.setReferenceAnswer(record.getReferenceAnswer());
            }

            return feedbackRes;
        } catch (Exception e) {
            log.error("生成反馈失败", e);
            record.setStatus(InterviewRecord.InterviewStatus.FAILED);
            interviewRecordRepository.save(record);
            throw new RuntimeException("生成反馈失败: " + e.getMessage(), e);
        }
    }

    private FeedbackRes parseAndSaveFeedback(InterviewRecord record, String jsonResponse) {
        try {
            // 清理可能的Markdown标记
            String cleanResponse = jsonResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            log.debug("清理后的响应: {}", cleanResponse.substring(0, Math.min(500, cleanResponse.length())));

            JSONObject json = new JSONObject(cleanResponse);

            // 更新面试记录
            record.setOverallScore(extractScore(json));
            record.setOverallFeedback(json.optString("overallFeedback", "暂无总体评价"));

            // 保存改进建议（JSON格式）
            if (json.has("improvementSuggestions")) {
                JSONArray suggestionsArray = json.getJSONArray("improvementSuggestions");
                record.setImprovementSuggestions(suggestionsArray.toString());
            }

            record.setStatus(InterviewRecord.InterviewStatus.FEEDBACK_GENERATED);
            interviewRecordRepository.save(record);

            // 保存维度评价（先删除旧的）
            feedbackDimensionRepository.deleteByInterviewId(record.getInterviewId());

            if (json.has("dimensions")) {
                JSONArray dimensionsArray = json.getJSONArray("dimensions");
                List<FeedbackDimension> dimensions = new ArrayList<>();

                // 定义标准的维度名称映射
                String[] standardDimensionNames = {"专业知识", "问题解决", "沟通表达", "技术深度", "逻辑思维"};

                for (int i = 0; i < dimensionsArray.length(); i++) {
                    JSONObject dimJson = dimensionsArray.getJSONObject(i);

                    FeedbackDimension dimension = new FeedbackDimension();
                    dimension.setInterviewRecord(record);
                    dimension.setInterviewId(record.getInterviewId());

                    // 使用标准维度名称，如果索引超出范围则使用AI返回的名称
                    String dimName;
                    if (i < standardDimensionNames.length) {
                        dimName = standardDimensionNames[i];
                    } else {
                        dimName = dimJson.optString("name", dimJson.optString("dimensionName", "维度" + (i + 1)));
                    }
                    dimension.setDimensionName(dimName);
                    dimension.setOrderNum(i);  // 设置显示顺序

                    Object scoreObj = dimJson.opt("score");
                    if (scoreObj instanceof Number) {
                        double scoreValue = ((Number) scoreObj).doubleValue();
                        dimension.setScore(BigDecimal.valueOf(scoreValue));
                    } else if (scoreObj instanceof String) {
                        try {
                            String scoreStr = (String) scoreObj;
                            String numericPart = scoreStr.replaceAll("[^0-9.]", "");
                            if (!numericPart.isEmpty()) {
                                double scoreValue = Double.parseDouble(numericPart);
                                dimension.setScore(BigDecimal.valueOf(scoreValue));
                            } else {
                                dimension.setScore(BigDecimal.ZERO);
                            }
                        } catch (NumberFormatException e) {
                            dimension.setScore(BigDecimal.ZERO);
                        }
                    } else {
                        dimension.setScore(BigDecimal.ZERO);
                    }

                    dimension.setEvaluation(dimJson.optString("evaluation", "暂无评价"));

                    // 设置权重（根据维度名称设置不同的权重）
                    if (dimName.contains("技术") || dimName.contains("专业")) {
                        dimension.setWeight(new BigDecimal("1.20"));
                    } else if (dimName.contains("沟通") || dimName.contains("表达")) {
                        dimension.setWeight(new BigDecimal("1.00"));
                    } else if (dimName.contains("问题解决") || dimName.contains("逻辑")) {
                        dimension.setWeight(new BigDecimal("1.10"));
                    } else {
                        dimension.setWeight(new BigDecimal("1.00"));
                    }

                    dimensions.add(dimension);
                }

                feedbackDimensionRepository.saveAll(dimensions);
            }

            // 构建返回的FeedbackRes对象
            FeedbackRes feedbackRes = new FeedbackRes();
            feedbackRes.setOverallScore(record.getOverallScore());
            feedbackRes.setOverallFeedback(record.getOverallFeedback());
            feedbackRes.setReferenceAnswer(record.getReferenceAnswer());
            // 设置维度评价
            List<FeedbackDimension> savedDimensions = feedbackDimensionRepository.findByInterviewId(record.getInterviewId());
            List<FeedbackRes.DimensionEvaluation> dimensionEvaluations = new ArrayList<>();

            for (FeedbackDimension dim : savedDimensions) {
                FeedbackRes.DimensionEvaluation de = new FeedbackRes.DimensionEvaluation();
                de.setDimensionName(dim.getDimensionName());
                de.setScore(String.format("%.1f", dim.getScore()));
                de.setEvaluation(dim.getEvaluation());
                dimensionEvaluations.add(de);
            }
            feedbackRes.setDimensions(dimensionEvaluations);

            // 设置改进建议
            if (record.getImprovementSuggestions() != null) {
                JSONArray suggestionsJson = new JSONArray(record.getImprovementSuggestions());
                List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < suggestionsJson.length(); i++) {
                    suggestions.add(suggestionsJson.getString(i));
                }
                feedbackRes.setImprovementSuggestions(suggestions);
            }



            return feedbackRes;

        } catch (Exception e) {
            log.error("解析或保存反馈失败", e);
            throw new RuntimeException("解析反馈数据失败: " + e.getMessage(), e);
        }
    }

    private String extractScore(JSONObject json) {
        try {
            if (json.has("overallScore")) {
                Object scoreObj = json.get("overallScore");
                if (scoreObj instanceof Number) {
                    double score = ((Number) scoreObj).doubleValue();
                    return String.format("%.1f/10", score);
                } else if (scoreObj instanceof String) {
                    String scoreStr = (String) scoreObj;
                    // 尝试提取数字
                    String numericPart = scoreStr.replaceAll("[^0-9.]", "");
                    if (!numericPart.isEmpty()) {
                        double score = Double.parseDouble(numericPart);
                        return String.format("%.1f/10", score);
                    }
                }
            }
            return "0.0/10";
        } catch (Exception e) {
            log.warn("提取分数失败", e);
            return "0.0/10";
        }
    }

    private Map<String, String> extractQuestionAndAnswerFromAIResponse(String aiResponse) {
        Map<String, String> result = new HashMap<>();

        try {
            log.debug("AI原始响应: {}", aiResponse);

            // 情况1：尝试解析JSON
            // 清理可能的Markdown标记和多余字符
            String cleanedResponse = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            log.debug("清理后响应: {}", cleanedResponse);

            // 尝试解析JSON
            JSONObject json = new JSONObject(cleanedResponse);

            // 提取question
            if (json.has("question")) {
                String question = json.getString("question");
                result.put("question", question);
                log.info("成功提取问题: {}", question.substring(0, Math.min(100, question.length())));
            }

            // 提取answer（确保只提取answer字段）
            if (json.has("answer")) {
                String answer = json.getString("answer");

                // 清理可能的额外字符和格式
                answer = cleanAnswerText(answer);
                result.put("answer", answer);
                log.info("成功提取答案: {}", answer.substring(0, Math.min(100, answer.length())));
            }

            // 如果成功提取了question和answer，直接返回
            if (result.containsKey("question") && result.containsKey("answer")) {
                return result;
            }

            // 情况2：如果不是标准JSON，尝试手动提取
            log.warn("AI响应不是标准JSON，尝试其他方式提取");

        } catch (Exception e) {
            log.warn("JSON解析失败，尝试其他方式提取: {}", e.getMessage());
        }

        // 情况3：尝试正则表达式提取
        try {
            // 清理响应文本
            String cleanText = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            // 提取question：尝试匹配 "question": "内容" 格式
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"question\":\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(cleanText);
            if (matcher.find()) {
                String question = matcher.group(1);
                result.put("question", question);
                log.info("正则提取问题: {}", question.substring(0, Math.min(100, question.length())));
            }

            // 提取question：尝试匹配 'question': '内容' 格式
            pattern = java.util.regex.Pattern.compile("'question':\\s*'([^']+)'");
            matcher = pattern.matcher(cleanText);
            if (matcher.find() && !result.containsKey("question")) {
                String question = matcher.group(1);
                result.put("question", question);
                log.info("正则提取问题（单引号）: {}", question.substring(0, Math.min(100, question.length())));
            }

            // 提取answer：尝试匹配 "answer": "内容" 格式
            pattern = java.util.regex.Pattern.compile("\"answer\":\\s*\"([^\"]+)\"");
            matcher = pattern.matcher(cleanText);
            if (matcher.find()) {
                String answer = matcher.group(1);
                // 清理可能的额外字符和格式
                answer = cleanAnswerText(answer);
                result.put("answer", answer);
                log.info("正则提取答案: {}", answer.substring(0, Math.min(100, answer.length())));
            }

            // 提取answer：尝试匹配 'answer': '内容' 格式
            pattern = java.util.regex.Pattern.compile("'answer':\\s*'([^']+)'");
            matcher = pattern.matcher(cleanText);
            if (matcher.find() && !result.containsKey("answer")) {
                String answer = matcher.group(1);
                // 清理可能的额外字符和格式
                answer = cleanAnswerText(answer);
                result.put("answer", answer);
                log.info("正则提取答案（单引号）: {}", answer.substring(0, Math.min(100, answer.length())));
            }

        } catch (Exception e) {
            log.warn("正则提取失败: {}", e.getMessage());
        }

        // 情况4：如果以上都失败，检查是否响应本身就是纯问题文本
        // 检查是否包含常见的JSON结构特征
        if (!aiResponse.contains("\"question\"") &&
                !aiResponse.contains("{") &&
                !aiResponse.contains("}")) {
            log.info("响应似乎是纯问题文本");
            result.put("question", aiResponse);
        }

        // 最终回退：如果没有提取到question，返回原始响应（截断）
        if (!result.containsKey("question")) {
            log.warn("无法提取问题，返回原始响应（截断）");
            String fallback = aiResponse.length() > 1000 ? aiResponse.substring(0, 1000) + "..." : aiResponse;
            result.put("question", fallback);
        }

        // 如果没有提取到answer，设置为空字符串
        if (!result.containsKey("answer")) {
            result.put("answer", "");
        }

        return result;
    }

    /**
     * 清理答案文本，移除可能的额外格式和标记
     */
    private String cleanAnswerText(String answer) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }

        // 移除可能的Markdown代码块标记
        String cleaned = answer
                .replace("```python", "")
                .replace("```java", "")
                .replace("```javascript", "")
                .replace("```", "")
                .trim();

        // 移除可能的JSON转义字符
        cleaned = cleaned
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t");

        return cleaned;
    }

    @Override
    @Async("asyncFeedbackExecutor")
    public CompletableFuture<FeedbackRes> generateStructuredFeedback(FeedbackReq request) {
        // 这里可以根据需要实现异步生成反馈的逻辑
        // 目前先调用同步方法实现
        try {
            // 由于FeedbackReq只包含question和answer，没有interviewId
            // 需要先创建或查找对应的面试记录
            String interviewId = "TEMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

            // 创建临时记录
            InterviewRecord record = new InterviewRecord();
            record.setInterviewId(interviewId);
            record.setCareerDirection("未知"); // 从请求中无法获取，设为默认
            record.setDifficultyLevel(2);
            record.setQuestionText(request.getQuestion());
            record.setAnswerText(request.getAnswer());
            record.setStatus(InterviewRecord.InterviewStatus.FEEDBACK_GENERATED);

            interviewRecordRepository.save(record);

            // 生成反馈
            FeedbackRes feedback = generateInterviewFeedback(interviewId, request.getAnswer());
            return CompletableFuture.completedFuture(feedback);
        } catch (Exception e) {
            log.error("异步生成反馈失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Object getInterviewHistory(String userId) {
        List<InterviewRecord> records = interviewRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (InterviewRecord record : records) {
            Map<String, Object> history = new HashMap<>();
            history.put("interviewId", record.getInterviewId());
            history.put("careerDirection", record.getCareerDirection());
            history.put("difficultyLevel", record.getDifficultyLevel());
            history.put("question", record.getQuestionText().length() > 100 ?
                    record.getQuestionText().substring(0, 100) + "..." : record.getQuestionText());
            history.put("overallScore", record.getOverallScore());
            history.put("status", record.getStatus().toString());
            history.put("createdAt", record.getCreatedAt());
            historyList.add(history);
        }

        return historyList;
    }

    // 新增方法：获取面试详情
    public InterviewRecord getInterviewDetail(String interviewId) {
        return interviewRecordRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new RuntimeException("未找到面试记录"));
    }

    // 新增方法：删除面试记录
    public void deleteInterview(String interviewId) {
        InterviewRecord record = interviewRecordRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new RuntimeException("未找到面试记录"));

        // 先删除关联的维度记录
        feedbackDimensionRepository.deleteByInterviewId(interviewId);

        // 再删除面试记录
        interviewRecordRepository.delete(record);
    }
}