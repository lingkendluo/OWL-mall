package com.owl.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对话消息")
public class ChatMessageDTO {

    @Schema(description = "system/user/assistant，服务端只采信 user 与 assistant")
    private String role;

    private String content;
}
