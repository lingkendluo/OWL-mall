package com.owl.catalog.vo;

import lombok.Data;

/**
 * 后台商品页左侧/顶部「大类索引」用的统计项。
 * 一次返回每个大类下的商品总数与在售/下架拆分，避免前端按分类逐个发请求。
 */
@Data
public class CategoryStatVO {

    private String code;

    private String name;

    /** 内联 SVG 标记，前端直接渲染，省一次映射 */
    private String glyph;

    /** 该大类下商品总数（不含已逻辑删除） */
    private Integer total;

    /** status = 1 的数量 */
    private Integer onSale;

    /** status = 0 的数量 */
    private Integer offSale;
}
