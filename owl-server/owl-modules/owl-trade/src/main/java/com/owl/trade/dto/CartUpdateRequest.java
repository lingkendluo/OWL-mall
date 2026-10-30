package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改购物车行（字段可选）")
public class CartUpdateRequest {

    private Integer qty;

    private Boolean checked;
}
