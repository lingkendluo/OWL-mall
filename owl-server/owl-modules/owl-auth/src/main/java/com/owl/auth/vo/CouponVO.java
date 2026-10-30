package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "我的优惠券")
public class CouponVO {

    @Schema(description = "用户券 id，字符串")
    private String id;

    private String name;

    @Schema(description = "1 满减 2 折扣")
    private Integer type;

    @Schema(description = "门槛，元；0 表示无门槛")
    private BigDecimal threshold;

    @Schema(description = "满减金额，元")
    private BigDecimal discount;

    @Schema(description = "生效时间，毫秒时间戳")
    private Long startAt;

    @Schema(description = "失效时间，毫秒时间戳")
    private Long endAt;

    @Schema(description = "0 未用 1 已用 2 过期")
    private Integer status;

    private Boolean usable;
}
