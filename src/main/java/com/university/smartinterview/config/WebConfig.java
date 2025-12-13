package com.university.smartinterview.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import com.university.smartinterview.Interceptor.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns("/user.html")  // 排除登录页面
                .excludePathPatterns("/api/auth/**")  // 排除认证接口
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png",
                        "/**/*.jpg", "/**/*.jpeg", "/**/*.gif",
                        "/**/*.ico", "/**/*.woff", "/**/*.woff2");  // 排除静态资源
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径重定向到登录页面
        registry.addViewController("/")
                .setViewName("redirect:/user.html");
        registry.addViewController("/start.html")
                .setViewName("forward:/static/start.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}