package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单列表项")
public class OrderBriefVO {

    private String orderNo;

    private Integer status;

    private String statusText;

    private BigDecimal payAmount;

    @Schema(description = "商品总件数")
    private Integer totalQty;

    private String firstItemName;

    private String coverImage;

    private Long createdAt;

    @Schema(description = "待付款订单的支付截止时间")
    private Long expireAt;
}
