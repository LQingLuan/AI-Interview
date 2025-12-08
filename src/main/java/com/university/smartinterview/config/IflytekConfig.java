// src/main/java/com/university/smartinterview/config/IflytekConfig.java
package com.university.smartinterview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "iflytek")
public class IflytekConfig {

    @Value("${iflytek.spark.api-url}")
    private String sparkApiUrl;

    @Value("${iflytek.spark.appid}")
    private String sparkAppId;

    @Value("${iflytek.spark.api-key}")
    private String sparkApiKey;

    @Value("${iflytek.spark.api-secret}")
    private String sparkApiSecret;

    private String appId;
    private String apiKey;
    private String apiSecret;
    private String apiUrl = "wss://iat-api.xfyun.cn/v2/iat";  // 前端需要

    // Getters and Setters
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    // 讯飞星火AI配置Bean（用于问题生成和反馈）
    @Bean
    public SparkAIConfig sparkAIConfig() {
        return new SparkAIConfig(sparkApiUrl, sparkAppId, sparkApiKey, sparkApiSecret);
    }

    // 讯飞星火配置参数类（保留）
    public static class SparkAIConfig {
        private final String apiUrl;
        private final String appId;
        private final String apiKey;
        private final String apiSecret;

        public SparkAIConfig(String apiUrl, String appId, String apiKey, String apiSecret) {
            this.apiUrl = apiUrl;
            this.appId = appId;
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        // Getters
        public String getApiUrl() { return apiUrl; }
        public String getAppId() { return appId; }
        public String getApiKey() { return apiKey; }
        public String getApiSecret() { return apiSecret; }
    }
}