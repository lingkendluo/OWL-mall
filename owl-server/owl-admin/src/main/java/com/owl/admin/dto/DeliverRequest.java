package com.owl.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单发货")
public class DeliverRequest {

    @Schema(description = "快递公司，默认顺丰")
    private String company;

    private String trackingNo;
}
