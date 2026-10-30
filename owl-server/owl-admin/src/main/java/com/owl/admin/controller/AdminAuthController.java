package com.owl.admin.controller;

import com.owl.admin.dto.AdminLoginRequest;
import com.owl.admin.service.AdminAuthService;
import com.owl.admin.vo.AdminLoginVO;
import com.owl.admin.vo.AdminVO;
import com.owl.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-登录")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    @Operation(summary = "当前管理员")
    @GetMapping("/me")
    public Result<AdminVO> me() {
        return Result.ok(adminAuthService.me());
    }
}
