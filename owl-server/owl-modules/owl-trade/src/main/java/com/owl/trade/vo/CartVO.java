package com.owl.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "购物车")
public class CartVO {

    private List<CartLineVO> lines;

    private CartSummaryVO summary;
}
