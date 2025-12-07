// src/main/java/com/university/smartinterview/config/WebSocketConfig.java
package com.university.smartinterview.config;

import com.university.smartinterview.WebSocket.InterviewWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.university.smartinterview.WebSocket.SessionIdHandshakeInterceptor;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 创建 WebSocketHandler bean
    @Bean(name = "interviewWebSocketHandler")
    public WebSocketHandler interviewWebSocketHandler() {
        return new InterviewWebSocketHandler();
    }

    // 创建 HandshakeInterceptor bean
    @Bean
    public SessionIdHandshakeInterceptor sessionIdHandshakeInterceptor() {
        return new SessionIdHandshakeInterceptor();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewWebSocketHandler(), "/api/ws/interview")
                .setAllowedOrigins("*") // 允许所有来源，生产环境应限制
                .addInterceptors(sessionIdHandshakeInterceptor());
    }
}