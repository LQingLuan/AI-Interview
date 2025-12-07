// src/main/java/com/university/smartinterview/task/TaskProgressTracker.java
package com.university.smartinterview.task;

import com.university.smartinterview.event.TaskUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务进度跟踪器
 */
@Component
public class TaskProgressTracker {

    // 任务状态
    public static class TaskStatus {
        private String taskId;
        private String taskType;
        private String description;
        private AtomicInteger progress = new AtomicInteger(0);
        private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
        private Object result;
        private String error;

        public TaskStatus(String taskId, String taskType, String description) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.description = description;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getTaskType() { return taskType; }
        public String getDescription() { return description; }
        public int getProgress() { return progress.get(); }
        public String getStatus() { return status; }
        public Object getResult() { return result; }
        public String getError() { return error; }

        public void updateProgress(int progress, String description) {
            this.progress.set(progress);
            if (description != null) {
                this.description = description;
            }
            this.status = "PROCESSING";
        }

        public void complete(Object result) {
            this.progress.set(100);
            this.status = "COMPLETED";
            this.result = result;
        }

        public void fail(String error) {
            this.status = "FAILED";
            this.error = error;
        }
    }

    // 存储所有任务的进度
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    // 修改构造函数：使用事件发布器代替WebSocketHandler
    @Autowired
    public TaskProgressTracker(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 启动新任务
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @param description 任务描述
     */
    public void startTask(String taskId, String taskType, String description) {
        TaskStatus status = new TaskStatus(taskId, taskType, description);
        taskStatusMap.put(taskId, status);
        // 发布事件代替直接调用WebSocketHandler
        eventPublisher.publishEvent(new TaskUpdateEvent(taskId, status));
    }

    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param message 进度消息
     */
    public void updateProgress(String taskId, int progress, String message) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.updateProgress(progress, message);
            // 发布事件代替直接调用WebSocketHandler
            eventPublisher.publishEvent(new TaskUpdateEvent(taskId, status));
        }
    }

    /**
     * 标记任务完成
     * @param taskId 任务ID
     * @param result 任务结果
     */
    public void completeTask(String taskId, Object result) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.complete(result);
            // 发布事件代替直接调用WebSocketHandler
            eventPublisher.publishEvent(new TaskUpdateEvent(taskId, status));

            // 保留一段时间后移除
            new Thread(() -> {
                try {
                    Thread.sleep(300000); // 保留5分钟
                    taskStatusMap.remove(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * 标记任务失败
     * @param taskId 任务ID
     * @param error 错误信息
     */
    public void failTask(String taskId, String error) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.fail(error);
            // 发布事件代替直接调用WebSocketHandler
            eventPublisher.publishEvent(new TaskUpdateEvent(taskId, status));

            // 保留一段时间后移除
            new Thread(() -> {
                try {
                    Thread.sleep(600000); // 保留10分钟
                    taskStatusMap.remove(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态对象
     */
    public TaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.get(taskId);
    }
}