package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车汇总。全部由服务端算，前端只展示 —— 两边各算一遍一定对不上账。
 */
@Data
@Schema(description = "购物车汇总")
public class CartSummaryVO {

    @Schema(description = "全部商品件数")
    private Integer count;

    private BigDecimal amount;

    @Schema(description = "比划线价省了多少")
    private BigDecimal saved;

    @Schema(description = "勾选件数")
    private Integer checkedCount;

    private BigDecimal checkedAmount;

    private Boolean allChecked;

    private Boolean isFreeShipping;

    @Schema(description = "还差多少免运费")
    private BigDecimal toFreeShipping;

    @Schema(description = "免运费进度，0-100")
    private Integer shippingRate;

    private BigDecimal freeShippingAt;

    private BigDecimal shipFee;
}
