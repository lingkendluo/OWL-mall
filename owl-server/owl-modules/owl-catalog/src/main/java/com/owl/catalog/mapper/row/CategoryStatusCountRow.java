package com.owl.catalog.mapper.row;

import lombok.Data;

/** 按「大类 + 上下架状态」聚合出来的商品数量，只给后台统计用 */
@Data
public class CategoryStatusCountRow {

    private Long categoryId;

    /** 1 在售 / 0 下架 */
    private Integer status;

    private Integer cnt;
}
