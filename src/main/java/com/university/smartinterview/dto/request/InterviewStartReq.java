// src/main/java/com/university/smartinterview/dto/request/InterviewStartReq.java
package com.university.smartinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public class InterviewStartReq {

    @NotBlank(message = "职业方向不能为空")
    private String careerDirection;

    @NotNull(message = "难度等级不能为空")
    @Range(min = 1, max = 3, message = "难度等级必须在1-3之间")
    private Integer difficultyLevel; // 1-初级, 2-中级, 3-高级

    // Getters and Setters
    public String getCareerDirection() {
        return careerDirection;
    }

    public void setCareerDirection(String careerDirection) {
        this.careerDirection = careerDirection;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
}