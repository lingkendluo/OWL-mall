package com.owl.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台操作日志")
public class AdminLogVO {

    @Schema(description = "日志 id，字符串")
    private String id;

    @Schema(description = "操作人账号")
    private String adminName;

    @Schema(description = "PRODUCT_UPDATE / ORDER_DELIVER …")
    private String action;

    @Schema(description = "操作对象，商品 code 或订单号")
    private String target;

    private String detail;

    private String ip;

    @Schema(description = "毫秒时间戳")
    private Long createdAt;

    @Schema(description = "展示文案，如「12 分钟前」")
    private String time;
}
