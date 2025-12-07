// src/main/java/com/university/smartinterview/event/TaskUpdateEvent.java
package com.university.smartinterview.event;

import com.university.smartinterview.task.TaskProgressTracker.TaskStatus;

public class TaskUpdateEvent {
    private final String taskId;
    private final TaskStatus status;

    public TaskUpdateEvent(String taskId, TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }
}