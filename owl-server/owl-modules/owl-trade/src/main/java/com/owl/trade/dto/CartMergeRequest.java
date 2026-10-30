package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录后合并本地购物车")
public class CartMergeRequest {

    private List<CartMergeLineDTO> lines;
}
