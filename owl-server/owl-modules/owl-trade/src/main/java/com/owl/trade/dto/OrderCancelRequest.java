package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "取消订单")
public class OrderCancelRequest {

    private String reason;
}
