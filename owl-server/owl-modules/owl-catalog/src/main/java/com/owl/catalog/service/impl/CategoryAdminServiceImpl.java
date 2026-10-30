package com.owl.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.catalog.dto.CategoryUpsertRequest;
import com.owl.catalog.dto.SubDTO;
import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.catalog.service.CategoryAdminService;
import com.owl.catalog.service.CategoryService;
import com.owl.catalog.vo.CategoryVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryAdminServiceImpl implements CategoryAdminService {

    private final CCategoryMapper categoryMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final CProductMapper productMapper;
    private final CategoryService categoryService;
    private final CacheService cache;

    @Override
    public List<CategoryVO> list() {
        return categoryService.listAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategoryUpsertRequest request) {
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getCode, request.getCode()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "类目 code 已存在");
        }
        CCategory category = new CCategory();
        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setHint(request.getHint() == null ? "" : request.getHint());
        category.setGlyph(request.getGlyph());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setStatus(1);
        categoryMapper.insert(category);

        upsertSubs(category.getId(), request.getSubs(), true);
        cache.evictAll();
        return categoryService.detail(category.getCode(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO update(String code, CategoryUpsertRequest request) {
        CCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getCode, code));
        if (category == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, "类目不存在");
        }
        category.setName(request.getName());
        if (request.getHint() != null) {
            category.setHint(request.getHint());
        }
        if (request.getGlyph() != null) {
            category.setGlyph(request.getGlyph());
        }
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        categoryMapper.updateById(category);

        upsertSubs(category.getId(), request.getSubs(), false);
        cache.evictAll();
        return categoryService.detail(category.getCode(), null);
    }

    /**
     * subs 全量覆盖：先删后插。仍有商品的细分不允许删（返回 30004），否则商品会挂在不存在的细分上。
     */
    private void upsertSubs(Long categoryId, List<SubDTO> subs, boolean isCreate) {
        if (subs == null) {
            return;
        }
        List<CSubCategory> existing = subCategoryMapper.selectList(new LambdaQueryWrapper<CSubCategory>()
                .eq(CSubCategory::getCategoryId, categoryId));
        Set<String> incoming = new HashSet<>();
        int sort = 0;
        for (SubDTO dto : subs) {
            if (dto.getCode() == null || dto.getCode().isBlank()) {
                continue;
            }
            incoming.add(dto.getCode());
            CSubCategory match = existing.stream()
                    .filter(item -> item.getCode().equals(dto.getCode()))
                    .findFirst().orElse(null);
            if (match == null) {
                CSubCategory sub = new CSubCategory();
                sub.setCategoryId(categoryId);
                sub.setCode(dto.getCode());
                sub.setName(dto.getName() == null ? dto.getCode() : dto.getName());
                sub.setSort(sort);
                sub.setStatus(1);
                subCategoryMapper.insert(sub);
            } else {
                match.setName(dto.getName() == null ? match.getName() : dto.getName());
                match.setSort(sort);
                subCategoryMapper.updateById(match);
            }
            sort += 10;
        }
        if (isCreate) {
            return;
        }
        for (CSubCategory sub : existing) {
            if (incoming.contains(sub.getCode())) {
                continue;
            }
            Long productCount = productMapper.selectCount(new LambdaQueryWrapper<CProduct>()
                    .eq(CProduct::getSubId, sub.getId()));
            if (productCount != null && productCount > 0) {
                throw new BizException(ErrorCode.CATEGORY_NOT_EMPTY,
                        "细分「" + sub.getName() + "」下还有 " + productCount + " 件商品，不能删除");
            }
            subCategoryMapper.deleteById(sub.getId());
        }
    }
}
