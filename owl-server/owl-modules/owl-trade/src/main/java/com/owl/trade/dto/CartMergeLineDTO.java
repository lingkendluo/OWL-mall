package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "合并行")
public class CartMergeLineDTO {

    private String productId;

    private Integer qty;
}
