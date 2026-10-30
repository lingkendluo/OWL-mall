package com.owl.catalog.service;

import com.owl.catalog.dto.AdminProductRequest;
import com.owl.catalog.vo.AdminProductVO;
import com.owl.catalog.vo.CategoryStatVO;
import com.owl.common.api.PageResult;

import java.util.List;

public interface ProductAdminService {

    /**
     * 后台商品列表。
     *
     * @param category 大类 code，null / 空串表示不限；用于顶部大类索引过滤
     */
    PageResult<AdminProductVO> list(String keyword, Integer status, String category, int page, int size);

    /** 大类维度统计：每个大类的商品总数与在售/下架拆分 */
    List<CategoryStatVO> categoryStats();

    AdminProductVO create(AdminProductRequest request);

    AdminProductVO update(String code, AdminProductRequest request);

    void delete(String code);

    AdminProductVO changeStatus(String code, int status);

    AdminProductVO adjustStock(String code, int delta, String reason);
}
