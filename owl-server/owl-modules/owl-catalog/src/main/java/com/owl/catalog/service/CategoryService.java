package com.owl.catalog.service;

import com.owl.catalog.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 15 个真实类目（不含「推荐」虚拟类目），每个带全部 8 个细分与件数 */
    List<CategoryVO> listAll();

    /** 类目详情：sub 有货只返回该细分，无货返回整类并 fallback=true */
    CategoryVO detail(String code, String sub);

    boolean exists(String code);
}
