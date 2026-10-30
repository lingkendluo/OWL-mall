package com.owl.assistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 回复")
public class ChatResponseVO {

    /** 前端 pickReply 认这个字段，任何时候都不能为空 */
    @Schema(description = "回复正文")
    private String reply;

    private String model;

    private UsageVO usage;

    /** true = 模型没调通，reply 是兜底文案 */
    @Schema(description = "是否降级回复")
    private Boolean degraded;
}
