package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "合并购物车结果")
public class CartMergeVO {

    private List<CartLineVO> lines;

    private CartSummaryVO summary;

    @Schema(description = "已下架或不存在而跳过的商品 code")
    private List<String> skipped;
}
