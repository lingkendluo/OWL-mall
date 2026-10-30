package com.owl.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "申请退款")
public class RefundRequest {

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    private String reason;
}
