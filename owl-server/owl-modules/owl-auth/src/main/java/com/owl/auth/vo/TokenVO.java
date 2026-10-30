package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "刷新 token 结果")
public class TokenVO {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;
}
