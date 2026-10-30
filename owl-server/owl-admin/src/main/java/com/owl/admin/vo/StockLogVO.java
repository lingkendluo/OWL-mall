package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "库存流水")
public class StockLogVO {

    private String id;

    @Schema(description = "正数入库，负数出库")
    private Integer changeQty;

    @Schema(description = "1 下单 2 取消回补 3 入库 4 后台调整")
    private Integer type;

    private String typeText;

    private Integer beforeQty;

    private Integer afterQty;

    @Schema(description = "关联单号")
    private String refNo;

    private Long createdAt;

    private String time;
}
