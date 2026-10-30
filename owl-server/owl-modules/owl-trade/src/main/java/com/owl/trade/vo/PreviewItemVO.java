package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "结算商品行")
public class PreviewItemVO {

    private String id;

    private String name;

    private String spec;

    private String image;

    private BigDecimal price;

    private BigDecimal list;

    private Integer qty;

    private BigDecimal total;
}
