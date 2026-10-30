package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新 token")
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
