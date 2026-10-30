package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录")
public class LoginRequest {

    @NotBlank(message = "请输入手机号或邮箱")
    private String account;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 只影响前端存 localStorage 还是 sessionStorage，后端不处理 */
    private Boolean remember = Boolean.TRUE;
}
