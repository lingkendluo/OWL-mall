package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单角标")
public class OrderCountVO {

    private Integer unpaid;

    private Integer shipping;

    private Integer review;

    private Integer after;
}
