package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "类目")
public class CategoryVO {

    @Schema(description = "类目 id，等于 code")
    private String id;

    private String name;

    private String hint;

    @Schema(description = "内联 SVG path，原样返回，前端用 v-html")
    private String glyph;

    @Schema(description = "该类目在售商品数")
    private Integer productCount;

    private List<SubVO> subs;

    @Schema(description = "类目详情页才返回；首页聚合里为空")
    private List<ProductVO> items;

    @Schema(description = "当前筛选的细分")
    private String currentSub;

    @Schema(description = "请求的细分无货时回落到整类，前端据此提示「还在补货」")
    private Boolean fallback;
}
