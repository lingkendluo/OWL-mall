package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "细分小类")
public class SubVO {

    @Schema(description = "细分 id，如 nut")
    private String id;

    private String name;

    @Schema(description = "该细分下的商品件数；0 表示补货中，前端会置灰")
    private Integer count;
}
