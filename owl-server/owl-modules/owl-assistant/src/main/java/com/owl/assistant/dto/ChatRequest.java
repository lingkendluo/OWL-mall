package com.owl.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 助手对话（前端原样发送，字段名不许改）")
public class ChatRequest {

    @Schema(description = "前端指定的模型，服务端以自己配置为准")
    private String model;

    private List<ChatMessageDTO> messages;

    private AssistContextDTO context;
}
