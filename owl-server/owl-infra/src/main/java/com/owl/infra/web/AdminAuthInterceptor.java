package com.owl.infra.web;

import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.context.AdminContext;
import com.owl.common.context.AdminUser;
import com.owl.infra.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * B 端鉴权。scope 必须是 admin，C 端 token 一律 403。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = JwtAuthInterceptor.resolveToken(request);
        if (token == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtService.parse(token);
        if (!JwtService.SCOPE_ADMIN.equals(jwtService.claim(claims, "scope"))) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!JwtService.TYPE_ACCESS.equals(jwtService.claim(claims, "typ"))) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String jti = claims.getId();
        if (jwtService.isRevoked(jti)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        AdminContext.set(new AdminUser(jwtService.subjectId(claims), jwtService.claim(claims, "username"),
                jwtService.claim(claims, "role"), jti));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }
}
