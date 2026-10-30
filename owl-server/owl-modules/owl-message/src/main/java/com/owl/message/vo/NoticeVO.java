package com.owl.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "公告")
public class NoticeVO {

    @Schema(description = "公告 id，字符串")
    private String id;

    private String title;

    private String body;

    @Schema(description = "1 公告 2 活动 3 维护 4 上新")
    private Integer type;

    @Schema(description = "类型中文，前端直接显示")
    private String typeText;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "0 草稿 1 已发布 2 已下线，用户端恒为 1")
    private Integer status;

    @Schema(description = "状态中文")
    private String statusText;

    @Schema(description = "毫秒时间戳")
    private Long publishAt;

    @Schema(description = "服务端格式化的展示文案，如「10 月 20 日」")
    private String time;

    @Schema(description = "毫秒时间戳")
    private Long createdAt;

    @Schema(description = "毫秒时间戳")
    private Long updatedAt;
}
