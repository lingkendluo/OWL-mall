package com.owl.auth.controller;

import com.owl.auth.dto.ChangePasswordRequest;
import com.owl.auth.dto.LoginRequest;
import com.owl.auth.dto.RefreshTokenRequest;
import com.owl.auth.dto.RegisterRequest;
import com.owl.auth.dto.SmsCodeRequest;
import com.owl.auth.dto.UpdateProfileRequest;
import com.owl.auth.service.AuthService;
import com.owl.auth.service.SmsCodeService;
import com.owl.auth.vo.LoginVO;
import com.owl.auth.vo.TokenVO;
import com.owl.auth.vo.UserVO;
import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public Result<Map<String, Object>> smsCode(@Valid @RequestBody SmsCodeRequest request, HttpServletRequest servlet) {
        SmsCodeService.SmsCodeResult result = smsCodeService.send(request.getPhone(), request.getScene(), clientIp(servlet));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expireIn", result.expireIn());
        // prod 环境这里是 null，前端不会拿到验证码
        if (result.code() != null) {
            data.put("code", result.code());
        }
        return Result.ok(data);
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "游客登录")
    @PostMapping("/guest")
    public Result<LoginVO> guest() {
        return Result.ok(authService.guest());
    }

    @Operation(summary = "刷新 token")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me(UserContext.requireUserId()));
    }

    @Operation(summary = "修改资料")
    @PatchMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(authService.updateProfile(UserContext.requireUserId(), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UserContext.requireUserId(), request);
        return Result.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
