package com.owl.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "后台新增/修改公告")
public class NoticeUpsertRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题最多 120 字")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String body;

    @Schema(description = "1 公告 2 活动 3 维护 4 上新，默认 1")
    private Integer type;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "保存后是否直接发布，默认 false（存草稿）")
    private Boolean publishNow;
}
