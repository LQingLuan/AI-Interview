// src/main/java/com/university/smartinterview/WebSocket/InterviewWebSocketHandler.java
package com.university.smartinterview.WebSocket;

import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.event.TaskUpdateEvent;
import com.university.smartinterview.task.TaskProgressTracker.TaskStatus;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.university.smartinterview.event.*;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("interviewWebSocketHandler")
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Async
    @EventListener
    public void handleTaskUpdateEvent(TaskUpdateEvent event) {
        this.sendTaskUpdate(event.getTaskId(), event.getStatus());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = getSessionId(session);
        if (sessionId != null) {
            sessionMap.put(sessionId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = getSessionId(session);
        if (sessionId != null) {
            sessionMap.remove(sessionId);
        }
    }

    public void sendTaskUpdate(String taskId, TaskStatus status) {
        WebSocketSession session = sessionMap.get(taskId);
        if (session != null && session.isOpen()) {
            sendTaskUpdate(session, status);
        }
    }

    private void sendTaskUpdate(WebSocketSession session, TaskStatus status) {
        try {
            String message = String.format(
                    "{\"type\":\"TASK_UPDATE\",\"taskId\":\"%s\",\"taskType\":\"%s\",\"progress\":%d,\"status\":\"%s\",\"description\":\"%s\"}",
                    status.getTaskId(), status.getTaskType(), status.getProgress(), status.getStatus(), status.getDescription()
            );
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            // 处理发送失败
        }
    }



    public void sendFeedbackComplete(String sessionId, FeedbackRes feedback) {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String message = String.format(
                        "{\"type\":\"FEEDBACK_COMPLETE\",\"overallScore\":\"%s\",\"overallFeedback\":\"%s\"}",
                        feedback.getOverallScore(),
                        escapeJson(feedback.getOverallFeedback())
                );
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                // 处理发送失败
            }
        }
    }

    public void sendError(String sessionId, String errorCode, String errorMessage) {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String message = String.format(
                        "{\"type\":\"ERROR\",\"code\":\"%s\",\"message\":\"%s\"}",
                        errorCode,
                        escapeJson(errorMessage)
                );
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                // 处理发送失败
            }
        }
    }

    private String getSessionId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        return (String) attributes.get("sessionId");
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // 添加事件监听方法
    @Async
    @EventListener
    public void handleFeedbackEvent(FeedbackEvent event) {
        this.sendFeedbackComplete(event.getSessionId(), event.getFeedback());
    }

    @Async
    @EventListener
    public void handleErrorEvent(ErrorEvent event) {
        this.sendError(event.getSessionId(), event.getErrorCode(), event.getErrorMessage());
    }

}