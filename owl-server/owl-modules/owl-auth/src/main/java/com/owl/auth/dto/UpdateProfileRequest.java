package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改资料（字段可选，只改传入的）")
public class UpdateProfileRequest {

    @Size(min = 1, max = 32, message = "昵称 1-32 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String avatar;
}
