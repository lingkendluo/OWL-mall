package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台商品")
public class AdminProductVO {

    @Schema(description = "商品 id，等于 code")
    private String id;

    private String name;

    private String categoryCode;

    private String subCode;

    private String spec;

    private BigDecimal price;

    private BigDecimal list;

    private String tag;

    private String emoji;

    private String image;

    private Integer stock;

    private Integer sales;

    private Integer status;

    private Boolean isFeatured;

    private ProductDetailVO detail;
}
