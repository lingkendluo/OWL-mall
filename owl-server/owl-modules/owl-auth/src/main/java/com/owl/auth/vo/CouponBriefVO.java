package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算页可用券。放在 auth 模块里，因为券的规则由 auth 负责，trade 只消费。
 */
@Data
@Schema(description = "结算页可用券")
public class CouponBriefVO {

    @Schema(description = "用户券 id，字符串")
    private String id;

    private String name;

    @Schema(description = "能减多少，元")
    private BigDecimal discount;

    private Boolean usable;

    @Schema(description = "不可用原因")
    private String reason;
}
