package com.university.smartinterview.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "interview_record", indexes = {
        @Index(name = "idx_interview_id", columnList = "interviewId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
public class InterviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", unique = true, nullable = false, length = 64)
    private String interviewId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "career_direction", nullable = false, length = 100)
    private String careerDirection;

    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @Lob
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Lob
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "overall_score", length = 20)
    private String overallScore;

    //新增
    @Column(name = "answer_duration")
    private Integer answerDuration;

    @Column(name = "ai_model_used", length = 50)
    private String aiModelUsed = "spark";

    @Column(name = "language", length = 10)
    private String language = "zh";

    @Column(name = "interview_mode", length = 20)
    private String interviewMode = "normal";

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    //结束

    @Lob
    @Column(name = "overall_feedback", columnDefinition = "TEXT")
    private String overallFeedback;

    @Column(name = "improvement_suggestions", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private String improvementSuggestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InterviewStatus status = InterviewStatus.PENDING;

    @OneToMany(mappedBy = "interviewRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FeedbackDimension> dimensions;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 枚举类
    public enum InterviewStatus {
        PENDING("待回答"),
        ANSWERED("已回答"),
        FEEDBACK_GENERATED("反馈已生成"),
        COMPLETED("已完成"),
        FAILED("失败");

        private final String description;

        InterviewStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}