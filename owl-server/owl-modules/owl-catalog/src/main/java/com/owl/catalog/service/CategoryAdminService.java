package com.owl.catalog.service;

import com.owl.catalog.dto.CategoryUpsertRequest;
import com.owl.catalog.vo.CategoryVO;

import java.util.List;

public interface CategoryAdminService {

    List<CategoryVO> list();

    CategoryVO create(CategoryUpsertRequest request);

    CategoryVO update(String code, CategoryUpsertRequest request);
}
