package com.owl.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.mapper.row.CategoryCountRow;
import com.owl.catalog.mapper.row.CategoryStatusCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CProductMapper extends BaseMapper<CProduct> {

    @Select("""
            SELECT category_id AS categoryId, COUNT(*) AS cnt
            FROM c_product
            WHERE status = 1 AND deleted = 0
            GROUP BY category_id
            """)
    List<CategoryCountRow> countByCategory();

    /** 后台分类索引：按大类 + 上下架状态一次聚合，避免每个分类发一次 count 查询 */
    @Select("""
            SELECT category_id AS categoryId, status AS status, COUNT(*) AS cnt
            FROM c_product
            WHERE deleted = 0
            GROUP BY category_id, status
            """)
    List<CategoryStatusCountRow> countByCategoryAndStatus();

    /**
     * 扣库存。必须带 stock >= qty 与 version 条件，影响 0 行就是被人抢走了，上层抛 30003。
     */
    @Update("""
            UPDATE c_product
            SET stock = stock - #{qty}, sales = sales + #{qty}, version = version + 1
            WHERE id = #{id} AND stock >= #{qty} AND version = #{version} AND status = 1 AND deleted = 0
            """)
    int deductStock(@Param("id") Long id, @Param("qty") int qty, @Param("version") int version);

    /** 取消 / 退款回补，销量同步减回去 */
    @Update("""
            UPDATE c_product
            SET stock = stock + #{qty}, sales = GREATEST(sales - #{qty}, 0), version = version + 1
            WHERE id = #{id}
            """)
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    /** 后台调整库存，delta 可正可负，但结果不能为负 */
    @Update("""
            UPDATE c_product
            SET stock = stock + #{delta}, version = version + 1
            WHERE id = #{id} AND stock + #{delta} >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    @Select("""
            SELECT MAX(CAST(SUBSTRING_INDEX(code, '-', -1) AS UNSIGNED))
            FROM c_product
            WHERE code LIKE CONCAT(#{categoryCode}, '-%')
            """)
    Integer maxSeqOfCategory(@Param("categoryCode") String categoryCode);

    @Select("""
            SELECT COUNT(*) FROM c_product WHERE status = 1 AND deleted = 0 AND stock < #{threshold}
            """)
    Long countLowStock(@Param("threshold") int threshold);
}
