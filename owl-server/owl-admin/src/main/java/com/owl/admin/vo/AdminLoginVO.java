package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员登录结果")
public class AdminLoginVO {

    private String accessToken;

    @Schema(description = "有效期，秒")
    private Long expiresIn;

    private AdminVO admin;
}
