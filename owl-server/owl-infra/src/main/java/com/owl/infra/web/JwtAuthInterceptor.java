package com.owl.infra.web;

import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.context.LoginUser;
import com.owl.common.context.UserContext;
import com.owl.infra.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * C 端鉴权。解析 Bearer token → UserContext；afterCompletion 必须 clear，否则线程复用会串号。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtService.parse(token);
        if (!JwtService.TYPE_ACCESS.equals(jwtService.claim(claims, "typ"))) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!JwtService.SCOPE_USER.equals(jwtService.claim(claims, "scope"))) {
            // B 端 token 不能访问 C 端接口
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        String jti = claims.getId();
        if (jwtService.isRevoked(jti)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        UserContext.set(new LoginUser(jwtService.subjectId(claims), jwtService.claimBoolean(claims, "guest"), jti));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
