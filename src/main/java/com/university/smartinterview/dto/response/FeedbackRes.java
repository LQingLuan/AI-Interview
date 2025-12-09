// src/main/java/com/university/smartinterview/dto/response/FeedbackRes.java
package com.university.smartinterview.dto.response;

import java.util.List;

public class FeedbackRes {

    private String overallScore; // 综合评分
    private String overallFeedback; // 总体反馈
    private List<DimensionEvaluation> dimensions; // 各维度评价
    private List<String> improvementSuggestions; // 改进建议
    private String referenceAnswer; // 示例回答

    // 维度评价内部类
    public static class DimensionEvaluation {
        private String dimensionName; // 维度名称
        private String score; // 维度得分（字符串格式）
        private String evaluation; // 维度评价

        // 添加权重字段
        private String weight;

        // Getters and Setters
        public String getDimensionName() {
            return dimensionName;
        }

        public void setDimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getEvaluation() {
            return evaluation;
        }

        public void setEvaluation(String evaluation) {
            this.evaluation = evaluation;
        }

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }
    }

    // Getters and Setters
    public String getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(String overallScore) {
        this.overallScore = overallScore;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }

    public List<DimensionEvaluation> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<DimensionEvaluation> dimensions) {
        this.dimensions = dimensions;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }
}