package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "积分流水")
public class PointsLogVO {

    private String id;

    @Schema(description = "变动值，正加负减")
    private Integer change;

    @Schema(description = "1 下单 2 评价 3 退款扣回 4 后台调整")
    private Integer type;

    private String remark;

    private Long createdAt;
}
