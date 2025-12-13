package com.university.smartinterview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
@ComponentScan(basePackages = {
        "com.university.smartinterview.controller",
        "com.university.smartinterview.service", // 添加服务包
        "com.university.smartinterview.config",
        "com.university.smartinterview.utils",
        "com.university.smartinterview.task"
})
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
@EnableAsync(proxyTargetClass = true) // 启用异步处理
@EnableWebSocket // 启用WebSocket支持
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SmartInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInterviewApplication.class, args);
        printStartupBanner();
    }

    /**
     * 打印启动横幅
     */
    private static void printStartupBanner() {
        System.out.println(
                        "╔═══════════════════════════════════════════════════════════════════════╗\n" +
                        "║                                                                       ║\n" +
                        "║    ██╗███╗   ██╗████████╗███████╗██████╗ ██╗███╗   ███╗ █████╗      ║\n" +
                        "║    ██║████╗  ██║╚══██╔══╝██╔════╝██╔══██╗██║████╗ ████║██╔══██╗     ║\n" +
                        "║    ██║██╔██╗ ██║   ██║   █████╗  ██████╔╝██║██╔████╔██║███████║     ║\n" +
                        "║    ██║██║╚██╗██║   ██║   ██╔══╝  ██╔══██╗██║██║╚██╔╝██║██╔══██║     ║\n" +
                        "║    ██║██║ ╚████║   ██║   ███████╗██║  ██║██║██║ ╚═╝ ██║██║  ██║     ║\n" +
                        "║    ╚═╝╚═╝  ╚═══╝   ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝╚═╝     ╚═╝╚═╝  ╚═╝     ║\n" +
                        "║                                                                       ║\n" +
                        "║    ╔═══════════════════════════════════════════════════════════╗      ║\n" +
                        "║    ║                智能面试平台 Smart Interview                 ║      ║\n" +
                        "║    ╚═══════════════════════════════════════════════════════════╝      ║\n" +
                        "║                                                                       ║\n" +
                        "║    :: Spring Boot ${spring-boot.formatted-version} ::                ║\n" +
                        "║    启动时间: ${application.formatted-startup-time}                     ║\n" +
                        "║                                                                       ║\n" +
                        "╚═══════════════════════════════════════════════════════════════════════╝");
    }

}