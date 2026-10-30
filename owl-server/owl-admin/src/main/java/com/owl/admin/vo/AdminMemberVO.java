package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台会员")
public class AdminMemberVO {

    @Schema(description = "会员 id，字符串")
    private String id;

    private String nickname;

    @Schema(description = "已打码手机号")
    private String phone;

    private String email;

    @Schema(description = "black / normal / guest")
    private String level;

    private String levelText;

    private String memberNo;

    private Integer points;

    @Schema(description = "余额，元")
    private BigDecimal balance;

    @Schema(description = "1 正常 0 禁用")
    private Integer status;

    private Boolean guest;

    @Schema(description = "毫秒时间戳")
    private Long createdAt;

    @Schema(description = "毫秒时间戳")
    private Long lastLoginAt;

    @Schema(description = "注册时间文案")
    private String time;
}
