package com.university.smartinterview.Interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 允许访问的路径（白名单）
        if (uri.equals("/") ||
                uri.equals("/user.html") ||
                uri.startsWith("/api/auth/") ||  // 所有认证相关的API
                uri.contains(".css") ||          // CSS文件
                uri.contains(".js") ||           // JavaScript文件
                uri.contains(".png") ||          // 图片资源
                uri.contains(".jpg") ||
                uri.contains(".jpeg") ||
                uri.contains(".gif") ||
                uri.contains(".ico")) {
            return true;
        }

        // 检查用户是否已登录
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            // 用户已登录
            return true;
        }
        response.sendRedirect("/user.html");
        return false;
    }
}