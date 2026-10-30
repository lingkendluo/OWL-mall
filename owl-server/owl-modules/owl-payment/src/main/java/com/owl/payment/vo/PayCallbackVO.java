package com.owl.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付回调结果")
public class PayCallbackVO {

    private String orderNo;

    @Schema(description = "1 已支付")
    private Integer payStatus;

    @Schema(description = "回调后的订单状态，成功时为 20")
    private Integer orderStatus;
}
