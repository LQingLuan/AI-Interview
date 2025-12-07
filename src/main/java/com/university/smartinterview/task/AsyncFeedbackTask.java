// src/main/java/com/university/smartinterview/task/AsyncFeedbackTask.java
package com.university.smartinterview.task;

import com.university.smartinterview.dto.request.FeedbackReq;
import com.university.smartinterview.dto.response.FeedbackRes;
import com.university.smartinterview.service.InterviewService;
import com.university.smartinterview.task.TaskProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;

import java.util.concurrent.CompletableFuture;

/**
 * 异步面试反馈生成任务
 */
@Component
public class AsyncFeedbackTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncFeedbackTask.class);

    private final InterviewService interviewService;
    private final WebSocketHandler webSocketHandler; // 改为接口类型
    private final TaskProgressTracker progressTracker;

    @Autowired
    public AsyncFeedbackTask(InterviewService interviewService,
                             @Qualifier("interviewWebSocketHandler") WebSocketHandler webSocketHandler, // 添加Qualifier
                             TaskProgressTracker progressTracker) {
        this.interviewService = interviewService;
        this.webSocketHandler = webSocketHandler;
        this.progressTracker = progressTracker;
    }

    /**
     * 异步生成面试反馈
     * @param sessionId 面试会话ID
     * @param question 面试问题
     * @param answer 用户回答
     * @param userId 用户ID
     * @return 生成的反馈结果
     */
    @Async("asyncFeedbackExecutor")
    public CompletableFuture<FeedbackRes> generateFeedbackAsync(
            String sessionId, String question, String answer, String userId) {

        try {
            // 创建任务进度
            progressTracker.startTask(sessionId, "feedback", "生成面试反馈");
            progressTracker.updateProgress(sessionId, 10, "开始分析回答...");

            // 构建反馈请求
            FeedbackReq request = new FeedbackReq();
            request.setQuestion(question);
            request.setAnswer(answer);

            // 模拟处理步骤
            simulateProcessingSteps(sessionId);

            // 生成结构化反馈
            logger.info("Generating feedback for session: {}", sessionId);
            FeedbackRes feedback = interviewService.generateStructuredFeedback(request).get();

            // 标记任务完成
            progressTracker.completeTask(sessionId, feedback);

            // 通过WebSocket通知前端
            sendFeedbackComplete(sessionId, feedback); // 调用本地方法发送

            logger.info("Feedback generated successfully for session: {}", sessionId);
            return CompletableFuture.completedFuture(feedback);
        } catch (Exception e) {
            progressTracker.failTask(sessionId, "反馈生成失败: " + e.getMessage());
            sendError(sessionId, "FEEDBACK_ERROR", "反馈生成失败");
            logger.error("Feedback generation failed for session: {}", sessionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 模拟处理步骤（实际项目中替换为真实处理逻辑）
     */
    private void simulateProcessingSteps(String sessionId) {
        try {
            progressTracker.updateProgress(sessionId, 20, "分析技术能力...");
            Thread.sleep(1000);

            progressTracker.updateProgress(sessionId, 40, "评估沟通表达...");
            Thread.sleep(1500);

            progressTracker.updateProgress(sessionId, 60, "检查问题解决能力...");
            Thread.sleep(1200);

            progressTracker.updateProgress(sessionId, 80, "生成改进建议...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送反馈完成通知
     */
    private void sendFeedbackComplete(String sessionId, FeedbackRes feedback) {
        // 通过事件发布器处理，不需要直接调用WebSocketHandler
        // 实际发送逻辑已由事件监听器处理
    }

    /**
     * 发送错误通知
     */
    private void sendError(String sessionId, String errorCode, String errorMessage) {
        // 通过事件发布器处理，不需要直接调用WebSocketHandler
        // 实际发送逻辑已由事件监听器处理
    }
}