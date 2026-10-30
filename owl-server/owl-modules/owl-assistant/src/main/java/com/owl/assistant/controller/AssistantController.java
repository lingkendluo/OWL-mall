package com.owl.assistant.controller;

import com.owl.assistant.dto.ChatRequest;
import com.owl.assistant.service.AssistantService;
import com.owl.assistant.vo.ChatResponseVO;
import com.owl.common.api.Result;
import com.owl.infra.security.JwtService;
import com.owl.infra.web.JwtAuthInterceptor;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物助手。接口公开：游客也能问，但限流按 IP 更严。
 * 带了合法 token 的自登录用户按 userId 限流，所以这里手动解一次 token，不引入强鉴权。
 */
@Slf4j
@Tag(name = "AI 助手")
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final JwtService jwtService;

    @Operation(summary = "购物助手对话")
    @PostMapping("/chat")
    public Result<ChatResponseVO> chat(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        return Result.ok(assistantService.chat(optionalUserId(servletRequest), clientIp(servletRequest), request));
    }

    /** 有 token 就认人，token 坏了也只是当游客，不影响对话 */
    private Long optionalUserId(HttpServletRequest request) {
        String token = JwtAuthInterceptor.resolveToken(request);
        if (token == null) {
            return null;
        }
        try {
            Claims claims = jwtService.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(jwtService.claim(claims, "typ"))
                    || !JwtService.SCOPE_USER.equals(jwtService.claim(claims, "scope"))) {
                return null;
            }
            return jwtService.subjectId(claims);
        } catch (Exception e) {
            log.debug("助手请求携带的 token 无效，按游客处理");
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
