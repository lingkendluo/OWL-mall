package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "加入购物车")
public class CartAddRequest {

    @NotBlank(message = "请选择商品")
    private String productId;

    @Schema(description = "数量，默认 1，越界取 1-99")
    private Integer qty = 1;
}
