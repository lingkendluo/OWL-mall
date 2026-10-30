package com.owl.admin.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.admin.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 后台审计。只在方法正常返回后记录，抛异常的操作不留痕（本来也没改成）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private static final int DETAIL_MAX = 500;

    private final AdminLogService adminLogService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(adminAction)", returning = "result")
    public void afterReturning(JoinPoint point, AdminAction adminAction, Object result) {
        adminLogService.record(adminAction.value(), target(point), detail(point, result), ip());
    }

    /** 约定：第一个 String 参数是操作对象（商品 code / 订单号 / 类目 code） */
    private String target(JoinPoint point) {
        for (Object arg : point.getArgs()) {
            if (arg instanceof String text && !text.isBlank()) {
                return truncate(text, 64);
            }
        }
        return null;
    }

    private String detail(JoinPoint point, Object result) {
        StringBuilder builder = new StringBuilder(point.getSignature().getName());
        for (Object arg : point.getArgs()) {
            if (arg == null || arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                continue;
            }
            try {
                builder.append(' ').append(objectMapper.writeValueAsString(arg));
            } catch (Exception e) {
                // 序列化不了就只留方法名，审计日志不值得为它报错
                log.debug("审计日志参数序列化失败 {}", arg.getClass().getSimpleName());
            }
        }
        return truncate(builder.toString(), DETAIL_MAX);
    }

    private String ip() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
