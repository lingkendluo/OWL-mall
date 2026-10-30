package com.owl.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.row.SubCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CSubCategoryMapper extends BaseMapper<CSubCategory> {

    /**
     * 细分件数实时算，不存表。
     * cnt=0 的细分必须保留 —— 前端要显示「补货中」，所以这里禁止加 HAVING cnt > 0。
     */
    @Select("""
            SELECT s.id AS id, s.code AS code, s.name AS name, COUNT(p.id) AS cnt
            FROM c_sub_category s
            LEFT JOIN c_product p ON p.sub_id = s.id AND p.status = 1 AND p.deleted = 0
            WHERE s.category_id = #{categoryId} AND s.status = 1
            GROUP BY s.id, s.code, s.name, s.sort
            ORDER BY s.sort
            """)
    List<SubCountRow> selectSubCounts(@Param("categoryId") Long categoryId);

    /** 首页一次查完 15 个类目的全部细分，避免 15 次循环查库 */
    @Select("""
            SELECT s.category_id AS categoryId, s.id AS id, s.code AS code, s.name AS name, COUNT(p.id) AS cnt
            FROM c_sub_category s
            LEFT JOIN c_product p ON p.sub_id = s.id AND p.status = 1 AND p.deleted = 0
            WHERE s.status = 1
            GROUP BY s.category_id, s.id, s.code, s.name, s.sort
            ORDER BY s.category_id, s.sort
            """)
    List<SubCountRow> selectAllSubCounts();
}
