package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "首页聚合")
public class HomeVO {

    private BrandVO brand;

    private List<CategoryVO> categories;

    private List<ProductVO> featured;

    @Schema(description = "免运费门槛，元")
    private BigDecimal freeShippingAt;

    private String shippingText;
}
