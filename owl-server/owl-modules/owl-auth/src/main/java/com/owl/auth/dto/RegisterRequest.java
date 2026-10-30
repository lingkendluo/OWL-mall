package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册")
public class RegisterRequest {

    @NotBlank(message = "请输入昵称")
    @Size(max = 32, message = "昵称最多 32 个字符")
    private String name;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请输入验证码")
    private String code;

    @NotBlank(message = "请输入密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "密码 8-20 位，必须同时含字母和数字")
    private String password;

    @NotNull(message = "请先同意用户协议")
    private Boolean agree;
}
