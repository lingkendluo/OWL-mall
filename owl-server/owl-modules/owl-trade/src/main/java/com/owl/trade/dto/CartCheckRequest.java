package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量勾选")
public class CartCheckRequest {

    @NotNull(message = "请传入勾选状态")
    private Boolean checked;

    @Schema(description = "不传表示全部行")
    private List<String> ids;
}
