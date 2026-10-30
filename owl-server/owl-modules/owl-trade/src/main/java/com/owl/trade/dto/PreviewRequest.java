package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "结算预览")
public class PreviewRequest {

    @NotEmpty(message = "请选择要结算的商品")
    private List<OrderLineDTO> lines;

    private String addressId;

    private String couponId;
}
