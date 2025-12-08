package com.university.smartinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnswerSubmitRequest {

    @NotBlank(message = "面试ID不能为空")
    private String interviewId;  // 改为interviewId

    @NotBlank(message = "用户回答不能为空")
    private String answerText;

    private String userId;  // 可选，用于关联用户
}