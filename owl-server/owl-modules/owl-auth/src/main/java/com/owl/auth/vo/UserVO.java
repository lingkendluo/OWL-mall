package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 字段名与前端 AuthUser 逐字对应，一个都不许改名。
 */
@Data
@Schema(description = "用户")
public class UserVO {

    @Schema(description = "用户 id，字符串")
    private String id;

    @Schema(description = "昵称")
    private String name;

    private String phone;

    private String email;

    @Schema(description = "black/normal/guest")
    private String level;

    private String memberNo;

    private Integer points;

    private Integer coupons;

    @Schema(description = "余额，元")
    private BigDecimal balance;

    @Schema(description = "注册时间，毫秒时间戳")
    private Long joinedAt;

    private Boolean isGuest;
}
