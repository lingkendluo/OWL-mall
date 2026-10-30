package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "修改密码")
public class ChangePasswordRequest {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "密码 8-20 位，必须同时含字母和数字")
    private String newPassword;
}
