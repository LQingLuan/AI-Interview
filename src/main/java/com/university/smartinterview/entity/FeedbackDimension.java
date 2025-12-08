// FeedbackDimension.java
package com.university.smartinterview.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feedback_dimension", indexes = {
        @Index(name = "idx_feedback_interview_id", columnList = "interviewId")
})
public class FeedbackDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_record_id", nullable = false)
    private InterviewRecord interviewRecord;

    @Column(name = "interview_id", nullable = false, length = 64)
    private String interviewId;

    @Column(name = "dimension_name", nullable = false, length = 50)
    private String dimensionName;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;  // 改为 BigDecimal

    @Lob
    @Column(name = "evaluation", columnDefinition = "TEXT")
    private String evaluation;

    // ============ 修改这里 ============
    @Column(name = "weight", precision = 3, scale = 2)
    private BigDecimal weight = new BigDecimal("1.00");  // 使用字符串构造函数避免精度问题

    @Column(name = "order_num")
    private Integer orderNum = 0;
    // =================================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 添加无参构造函数
    public FeedbackDimension() {
        this.weight = new BigDecimal("1.00");
    }
}