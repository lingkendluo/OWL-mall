package com.owl.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "后台新增/修改类目")
public class CategoryUpsertRequest {

    @NotBlank(message = "请输入类目 code")
    private String code;

    @NotBlank(message = "请输入类目名")
    private String name;

    private String hint;

    private String glyph;

    private Integer sort = 0;

    @Schema(description = "全量覆盖：先删后插，仍有商品的细分不允许删")
    private List<SubDTO> subs;
}
