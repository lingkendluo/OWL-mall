package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "账户资产")
public class AssetsVO {

    private Integer coupons;

    private Integer points;

    private BigDecimal balance;
}
