package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "运费信息")
public class ShippingVO {

    private BigDecimal freeShippingAt;

    private BigDecimal toFreeShipping;

    private Boolean isFreeShipping;
}
