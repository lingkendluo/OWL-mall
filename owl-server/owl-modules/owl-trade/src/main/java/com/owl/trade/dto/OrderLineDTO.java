package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 下单行。只有「买什么、买几件」，价格一律由服务端查库，前端传什么都不认。
 */
@Data
@Schema(description = "下单行")
public class OrderLineDTO {

    private String productId;

    private Integer qty;
}
