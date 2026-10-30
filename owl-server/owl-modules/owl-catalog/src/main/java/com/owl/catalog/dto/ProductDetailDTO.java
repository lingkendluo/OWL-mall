package com.owl.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "商品详情四段")
public class ProductDetailDTO {

    private String slogan;

    private String intro;

    /** 用 LinkedHashMap 接收，保留后台填写的键顺序 */
    private Map<String, String> facts = new LinkedHashMap<>();

    private List<String> tips;
}
