package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台概览")
public class StatsVO {

    @Schema(description = "累计成交额，元；只算已支付且未退款")
    private BigDecimal gmv;

    private Long orderCount;

    private Long userCount;

    private Long productCount;

    @Schema(description = "库存 < 10 的在售商品数")
    private Long lowStockCount;

    private Long todayOrderCount;

    private BigDecimal todayGmv;
}
