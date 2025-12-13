package com.university.smartinterview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String username;
    private Integer userId;  // 将String改为Integer类型
    private String userType;

    // 为了向后兼容，添加一个获取字符串类型ID的方法
    public String getUserIdAsString() {
        return userId != null ? userId.toString() : null;
    }
}