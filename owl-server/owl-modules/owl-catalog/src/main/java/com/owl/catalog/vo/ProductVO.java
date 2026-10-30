package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品")
public class ProductVO {

    @Schema(description = "商品 id，等于 code，如 snacks-1")
    private String id;

    private String name;

    private String spec;

    @Schema(description = "售价，元")
    private BigDecimal price;

    @Schema(description = "划线价，元")
    private BigDecimal list;

    private String tag;

    private String emoji;

    private String image;

    @Schema(description = "所属细分 code")
    /** 所属大类 code，详情页面包屑据此回分类页 */
    private String category;

    private String sub;

    private ProductDetailVO detail;
}
