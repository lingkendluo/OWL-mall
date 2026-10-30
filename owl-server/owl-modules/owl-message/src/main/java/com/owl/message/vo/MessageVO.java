package com.owl.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "站内信")
public class MessageVO {

    @Schema(description = "消息 id，字符串")
    private String id;

    @Schema(description = "order/promo/system")
    private String kind;

    private String title;

    private String body;

    @Schema(description = "服务端格式化的展示文案，如「12 分钟前」")
    private String time;

    private Boolean read;

    @Schema(description = "毫秒时间戳")
    private Long createdAt;

    @Schema(description = "关联订单号，点进去能跳")
    private String bizNo;
}
