package com.owl.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品详情 = Product 全字段 + 同类推荐。继承而不是嵌套，前端可以直接读平铺的字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品详情（含同类推荐）")
public class ProductDetailBundleVO extends ProductVO {

    @Schema(description = "同类推荐，最多 4 件，不含自己")
    private List<ProductVO> siblings;
}
