package com.university.smartinterview.controller;

import com.university.smartinterview.common.ApiResponse;
import com.university.smartinterview.dto.request.LoginRequest;
import com.university.smartinterview.dto.request.RegisterRequest;
import com.university.smartinterview.dto.response.CheckResponse;
import com.university.smartinterview.dto.response.LoginResponse;
import com.university.smartinterview.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理时，第一个IP为真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("用户登录请求: {}", request.getUsername());

        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = userService.login(request, ipAddress);

        if (response.isSuccess()) {
            // 将用户信息存入Session
            session.setAttribute("userId", response.getUserId());
            session.setAttribute("username", response.getUsername());
            session.setAttribute("userType", response.getUserType());
            session.setAttribute("isLoggedIn", true);

            // 设置Session过期时间（例如30分钟）
            session.setMaxInactiveInterval(30 * 60);

            // 返回用户信息
            Map<String, Object> result = Map.of(
                    "username", response.getUsername(),
                    "userId", response.getUserId(),
                    "userType", response.getUserType(),
                    "message", response.getMessage()
            );

            return ResponseEntity.ok(ApiResponse.success("登录成功", result));
        } else {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(response.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpSession session) {

        log.info("用户注册请求: {}", request.getUsername());

        LoginResponse response = userService.register(request);

        if (response.isSuccess()) {
            // 注册成功后自动登录，将用户信息存入Session
            session.setAttribute("userId", response.getUserId());
            session.setAttribute("username", response.getUsername());
            session.setAttribute("userType", response.getUserType());
            session.setAttribute("isLoggedIn", true);

            // 设置Session过期时间（例如30分钟）
            session.setMaxInactiveInterval(30 * 60);

            // 返回用户信息
            Map<String, Object> result = Map.of(
                    "username", response.getUsername(),
                    "userId", response.getUserId(),
                    "userType", response.getUserType(),
                    "message", response.getMessage()
            );

            return ResponseEntity.ok(ApiResponse.success("注册成功", result));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        // 使Session无效
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.success("退出登录成功", null));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<CheckResponse>> checkUsername(
            @RequestParam String username) {

        boolean available = userService.isUsernameAvailable(username);

        CheckResponse response = new CheckResponse();
        response.setAvailable(available);
        response.setUsername(username);

        return ResponseEntity.ok(ApiResponse.success("检查成功", response));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckResponse>> checkEmail(
            @RequestParam String email) {

        boolean available = userService.isEmailAvailable(email);

        CheckResponse response = new CheckResponse();
        response.setAvailable(available);
        response.setEmail(email);

        return ResponseEntity.ok(ApiResponse.success("检查成功", response));
    }

    @GetMapping("/user-info")
    public String getUserInfo(Model model, HttpSession session) {
        // 从session中获取用户ID
        Object userIdObj = session.getAttribute("userId");

        if (userIdObj == null) {
            log.warn("用户未登录，重定向到登录页面");
            return "redirect:/login"; // 重定向到登录页面
        }

        try {
            // userId在Session中是Integer类型，但需要转换为Long
            Integer userIdInt = (Integer) userIdObj;
            Long userId = userIdInt.longValue();

            log.info("获取用户信息，用户ID: {}", userId);

            // 获取用户信息（现在使用Long类型）
            Object user = userService.getUserById(userId);
            model.addAttribute("user", user);

            // 返回用户信息页面
            return "auth/user-info";

        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            model.addAttribute("error", "获取用户信息失败");
            return "error";
        }
    }

    @GetMapping("/session-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSession(HttpSession session) {

        Object isLoggedInObj = session.getAttribute("isLoggedIn");

        if (isLoggedInObj != null && (Boolean) isLoggedInObj) {
            // 用户已登录，返回用户信息
            Map<String, Object> sessionInfo = Map.of(
                    "isLoggedIn", true,
                    "userId", session.getAttribute("userId"),
                    "username", session.getAttribute("username"),
                    "userType", session.getAttribute("userType")
            );

            return ResponseEntity.ok(ApiResponse.success("用户已登录", sessionInfo));
        }

        // 用户未登录
        return ResponseEntity.ok(ApiResponse.success("用户未登录",
                Map.of("isLoggedIn", false)));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemInfo() {
        Map<String, Object> info = Map.of(
                "service", "Smart Interview System",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now(),
                "status", "running",
                "description", "智能面试系统认证服务",
                "authType", "Session认证"
        );

        log.info("系统信息查询");
        return ResponseEntity.ok(ApiResponse.success("获取系统信息成功", info));
    }

    @GetMapping("/auth-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthInfo(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        Map<String, Object> authInfo = Map.of(
                "module", "Authentication Module",
                "endpoints", Map.of(
                        "login", "/api/auth/login",
                        "register", "/api/auth/register",
                        "logout", "/api/auth/logout",
                        "userInfo", "/api/auth/user-info",
                        "sessionCheck", "/api/auth/session-check"
                ),
                "clientIp", clientIp,
                "timestamp", LocalDateTime.now(),
                "features", "用户认证、注册、Session管理",
                "note", "此版本使用Session认证"
        );

        log.info("认证模块信息查询，客户端IP: {}", clientIp);
        return ResponseEntity.ok(ApiResponse.success("获取认证模块信息成功", authInfo));
    }
}