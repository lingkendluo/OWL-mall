package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单行（快照，不随商品改价变化）")
public class OrderItemVO {

    @Schema(description = "商品 code")
    private String id;

    private String name;

    private String spec;

    private String image;

    private BigDecimal price;

    private BigDecimal list;

    private Integer qty;

    private BigDecimal total;
}
