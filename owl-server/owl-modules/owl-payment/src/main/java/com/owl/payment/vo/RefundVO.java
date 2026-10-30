package com.owl.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "退款单")
public class RefundVO {

    private String refundNo;

    private BigDecimal amount;

    @Schema(description = "0 处理中 1 成功 2 失败")
    private Integer status;
}
