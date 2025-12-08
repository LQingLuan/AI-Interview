package com.university.smartinterview.controller;

import com.university.smartinterview.dto.response.ResponseWrapper;
import com.university.smartinterview.task.TaskProgressTracker;
import com.university.smartinterview.task.TaskProgressTracker.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务状态查询控制器
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskProgressTracker progressTracker;

    @Autowired
    public TaskController(TaskProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ResponseWrapper<TaskStatus>> getTaskStatus(@PathVariable String taskId) {
        TaskStatus status = progressTracker.getTaskStatus(taskId);
        if (status != null) {
            return ResponseEntity.ok(ResponseWrapper.success(status));
        } else {
            return ResponseEntity.status(404)
                    .body(ResponseWrapper.error(404, "任务不存在或已过期"));
        }
    }
}