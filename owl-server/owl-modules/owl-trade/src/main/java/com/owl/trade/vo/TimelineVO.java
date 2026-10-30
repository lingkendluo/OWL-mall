package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单时间线")
public class TimelineVO {

    private Integer status;

    private String statusText;

    private Long at;

    private String remark;
}
