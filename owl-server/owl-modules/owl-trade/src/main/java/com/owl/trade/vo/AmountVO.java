package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "金额明细")
public class AmountVO {

    @Schema(description = "商品合计")
    private BigDecimal goodsAmount;

    @Schema(description = "优惠合计")
    private BigDecimal discount;

    private BigDecimal shipFee;

    @Schema(description = "应付 = 商品 - 优惠 + 运费")
    private BigDecimal payable;

    @Schema(description = "比划线价省了多少")
    private BigDecimal saved;
}
