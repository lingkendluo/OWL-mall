package com.owl.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "模拟支付回调")
public class MockCallbackRequest {

    @NotBlank(message = "payNo 不能为空")
    private String payNo;

    @NotBlank(message = "mockToken 不能为空")
    private String mockToken;

    private Boolean success = Boolean.TRUE;
}
