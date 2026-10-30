package com.owl.infra.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接口前缀 + 鉴权拦截 + CORS。
 *
 * 不用 server.servlet.context-path：那会把 /static 的图片也加上 /api/v1，
 * 前端所有商品图地址都要跟着改。这里只给 @RestController 加前缀。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** C 端免鉴权白名单（路径已含 /api/v1 前缀） */
    public static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/guest",
            "/api/v1/auth/refresh",
            "/api/v1/auth/sms-code",
            "/api/v1/c/**",
            "/api/v1/assistant/chat",
            "/api/v1/pay/mock/callback",
            "/api/v1/health",
            "/doc.html",
            "/doc.html/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/favicon.ico",
            "/error",
            "/static/**"
    };

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(PUBLIC_PATHS)
                .excludePathPatterns("/api/v1/admin/**")
                .order(1);

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/auth/login")
                .order(2);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
