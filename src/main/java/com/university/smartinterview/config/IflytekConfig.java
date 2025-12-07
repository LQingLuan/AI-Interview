// src/main/java/com/university/smartinterview/config/IflytekConfig.java
package com.university.smartinterview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@ConfigurationProperties(prefix = "iflytek")
public class IflytekConfig {

    @Value("${iflytek.spark.api-url}")
    private String sparkApiUrl;

    @Value("${iflytek.speech.ws-url}")
    private String speechWsUrl;



    @Value("${iflytek.spark.appid}")
    private String sparkAppId;

    @Value("${iflytek.spark.api-key}")
    private String sparkApiKey;

    @Value("${iflytek.spark.api-secret}")
    private String sparkApiSecret;

    @Value("${iflytek.speech.format}")
    private String audioFormat;

    @Value("${iflytek.speech.domain}")
    private String speechDomain;

    @Value("${iflytek.speech.accent}")
    private String speechAccent;



    private String appId;
    private String apiKey;
    private String apiSecret;
    private String apiUrl = "wss://iat-api.xfyun.cn/v2/iat";

    // Getters and Setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    // 讯飞星火AI配置Bean
    @Bean
    public SparkAIConfig sparkAIConfig() {
        return new SparkAIConfig(sparkApiUrl, sparkAppId, sparkApiKey, sparkApiSecret);
    }

    // 语音识别配置Bean
    @Bean
    public SpeechRecognitionConfig speechConfig() {
        return new SpeechRecognitionConfig(speechWsUrl, audioFormat, speechDomain, speechAccent);
    }

    // WebSocket客户端Bean
    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }

    // 讯飞星火配置参数类
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

    // 语音识别配置参数类
    public static class SpeechRecognitionConfig {
        private final String wsUrl;
        private final String audioFormat;
        private final String domain;
        private final String accent;

        public SpeechRecognitionConfig(String wsUrl, String audioFormat, String domain, String accent) {
            this.wsUrl = wsUrl;
            this.audioFormat = audioFormat;
            this.domain = domain;
            this.accent = accent;
        }

        // Getters
        public String getWsUrl() { return wsUrl; }
        public String getAudioFormat() { return audioFormat; }
        public String getDomain() { return domain; }
        public String getAccent() { return accent; }
    }


}