package com.owl.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "支付单")
public class PayDetailVO {

    private String payNo;

    private String orderNo;

    private String channel;

    private BigDecimal amount;

    @Schema(description = "0 待支付 1 成功 2 失败 3 关闭 4 已退款")
    private Integer status;

    private Long paidAt;
}
