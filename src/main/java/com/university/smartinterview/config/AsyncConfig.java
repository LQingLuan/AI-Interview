// src/main/java/com/university/smartinterview/config/AsyncConfig.java
package com.university.smartinterview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

    @Bean(name = "asyncFeedbackExecutor")
    public Executor asyncFeedbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("Async-Feedback-");
        // 拒绝策略：当队列满时，由调用线程处理该任务
        executor.setRejectedExecutionHandler((r, executor1) -> {
            if (!executor1.isShutdown()) {
                try {
                    // 尝试放入队列
                    executor1.getQueue().put(r);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        executor.initialize();
        return executor;
    }
}