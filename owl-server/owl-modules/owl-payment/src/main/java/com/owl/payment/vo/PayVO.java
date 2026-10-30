package com.owl.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "发起支付结果")
public class PayVO {

    private String payNo;

    private String channel;

    @Schema(description = "金额，元")
    private BigDecimal amount;

    @Schema(description = "模拟渠道凭证，回调时要带回来")
    private String mockToken;

    private Long expireAt;
}
