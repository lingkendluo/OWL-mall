package com.owl.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台新增/修改商品")
public class AdminProductRequest {

    @NotBlank(message = "请输入商品名")
    private String name;

    @NotBlank(message = "请选择类目")
    private String categoryCode;

    @NotBlank(message = "请选择细分")
    private String subCode;

    private String spec;

    @NotNull(message = "请输入售价")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    @NotNull(message = "请输入划线价")
    @DecimalMin(value = "0.01", message = "划线价必须大于 0")
    private BigDecimal list;

    private String tag;

    private String emoji;

    private String image;

    private Integer stock = 0;

    private Boolean isFeatured = Boolean.FALSE;

    private ProductDetailDTO detail;
}
