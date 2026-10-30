package com.owl.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "库存调整")
public class StockAdjustRequest {

    @NotNull(message = "请输入调整数量")
    @Min(value = -100000, message = "调整数量过大")
    @Max(value = 100000, message = "调整数量过大")
    private Integer delta;

    private String reason;
}
