package com.owl.assistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "token 用量")
public class UsageVO {

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;
}
