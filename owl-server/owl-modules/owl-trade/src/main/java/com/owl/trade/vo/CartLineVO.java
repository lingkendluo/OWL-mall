package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "购物车行")
public class CartLineVO {

    @Schema(description = "商品 code，如 snacks-1")
    private String id;

    private String name;

    private String spec;

    private BigDecimal price;

    private BigDecimal list;

    private String emoji;

    private String image;

    private Integer qty;

    private Boolean checked;

    @Schema(description = "商品已下架或已删除，前端置灰")
    private Boolean invalid;
}
