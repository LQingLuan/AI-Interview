// src/main/java/com/university/smartinterview/task/AsyncSpeechProcessingTask.java
package com.university.smartinterview.task;

import com.university.smartinterview.dto.response.SpeechRecognitionRes;
import com.university.smartinterview.service.SpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.WebSocketHandler;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncSpeechProcessingTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSpeechProcessingTask.class);

    private final SpeechService speechService;
    private final WebSocketHandler webSocketHandler;
    private final TaskProgressTracker progressTracker;

    @Autowired
    public AsyncSpeechProcessingTask(SpeechService speechService,
                                     @Qualifier("interviewWebSocketHandler") WebSocketHandler webSocketHandler,
                                     TaskProgressTracker progressTracker) {
        this.speechService = speechService;
        this.webSocketHandler = webSocketHandler;
        this.progressTracker = progressTracker;
    }

    @Async("speechProcessingExecutor")
    public CompletableFuture<SpeechRecognitionRes> processStreamingRecognition(
            String sessionId, byte[] audioData) {

        try {
            progressTracker.startTask(sessionId, "speech", "语音识别处理");
            progressTracker.updateProgress(sessionId, 5, "开始处理音频...");

            logger.info("Processing streaming audio for session: {}, size: {} bytes",
                    sessionId, audioData.length);

            SpeechRecognitionRes result = speechService.streamingRecognition(sessionId, audioData).get();

            if (result.isFinal()) {
                progressTracker.completeTask(sessionId, result.getFinalResult());
            } else {
                progressTracker.updateProgress(sessionId, result.getProgress(),
                        "部分结果: " + result.getPartialResult());
            }

            // 通过事件机制处理，不再直接调用
            // 实际发送由事件监听器处理

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            progressTracker.failTask(sessionId, "语音识别失败: " + e.getMessage());
            // 通过事件机制处理错误
            // 实际发送由事件监听器处理
            logger.error("Speech processing failed for session: {}", sessionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("speechProcessingExecutor")
    public CompletableFuture<SpeechRecognitionRes> processFileRecognition(MultipartFile file) {
        String sessionId = "FILE_" + System.currentTimeMillis();

        try {
            progressTracker.startTask(sessionId, "speech", "文件语音识别");
            progressTracker.updateProgress(sessionId, 10, "上传文件处理中...");

            logger.info("Processing audio file: {}, size: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            SpeechRecognitionRes result = speechService.fileRecognition(file);
            progressTracker.completeTask(sessionId, result.getFinalResult());

            // 通过事件机制处理，不再直接调用
            // 实际发送由事件监听器处理

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            progressTracker.failTask(sessionId, "文件语音识别失败: " + e.getMessage());
            logger.error("File speech recognition failed: {}", file.getOriginalFilename(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

