package com.owl.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "助手上下文里的购物车行")
public class AssistLineDTO {

    private String name;

    private Integer qty;

    private BigDecimal price;
}
