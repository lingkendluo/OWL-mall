package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "下单结果")
public class OrderCreateVO {

    private String orderNo;

    private String payNo;

    private BigDecimal payAmount;

    private Integer status;

    private Long expireAt;
}
