package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商品详情四段")
public class ProductDetailVO {

    private String slogan;

    private String intro;

    @Schema(description = "有序对象，键顺序即展示顺序")
    private java.util.Map<String, String> facts;

    private List<String> tips;
}
