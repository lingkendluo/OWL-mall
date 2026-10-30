package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "access 有效期，秒", example = "7200")
    private Long expiresIn;

    private UserVO user;
}
