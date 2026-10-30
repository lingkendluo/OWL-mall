package com.owl.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.catalog.converter.ProductAssembler;
import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.catalog.mapper.row.CategoryCountRow;
import com.owl.catalog.mapper.row.SubCountRow;
import com.owl.catalog.service.CategoryService;
import com.owl.catalog.vo.CategoryVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.catalog.vo.SubVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    static final String CACHE_CATEGORIES = "categories";
    static final Duration TTL_CATEGORIES = Duration.ofMinutes(10);

    private final CCategoryMapper categoryMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final CProductMapper productMapper;
    private final ProductAssembler assembler;
    private final CacheService cache;

    @Override
    public List<CategoryVO> listAll() {
        return cache.getOrLoad(CACHE_CATEGORIES, TTL_CATEGORIES, this::buildAll);
    }

    private List<CategoryVO> buildAll() {
        List<CCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getStatus, 1)
                .orderByAsc(CCategory::getSort)
                .orderByAsc(CCategory::getId));
        if (categories.isEmpty()) {
            return List.of();
        }
        // 一条 GROUP BY 拿全部细分的件数，别在循环里查库
        Map<Long, List<SubCountRow>> subsByCategory = subCategoryMapper.selectAllSubCounts().stream()
                .collect(Collectors.groupingBy(SubCountRow::getCategoryId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Integer> productCounts = productMapper.countByCategory().stream()
                .collect(Collectors.toMap(CategoryCountRow::getCategoryId, CategoryCountRow::getCnt, (a, b) -> a));

        List<CategoryVO> result = new ArrayList<>(categories.size());
        for (CCategory category : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(category.getCode());
            vo.setName(category.getName());
            vo.setHint(category.getHint());
            vo.setGlyph(category.getGlyph());
            vo.setProductCount(productCounts.getOrDefault(category.getId(), 0));
            vo.setItems(List.of());
            vo.setFallback(false);
            List<SubVO> subs = new ArrayList<>();
            for (SubCountRow row : subsByCategory.getOrDefault(category.getId(), List.of())) {
                SubVO sub = new SubVO();
                sub.setId(row.getCode());
                sub.setName(row.getName());
                sub.setCount(row.getCnt() == null ? 0 : row.getCnt());
                subs.add(sub);
            }
            vo.setSubs(subs);
            result.add(vo);
        }
        return result;
    }

    @Override
    public CategoryVO detail(String code, String sub) {
        CCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getCode, code)
                .eq(CCategory::getStatus, 1));
        if (category == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, "类目不存在");
        }

        CategoryVO vo = listAll().stream()
                .filter(item -> item.getId().equals(code))
                .findFirst()
                .map(this::copy)
                .orElseThrow(() -> new BizException(ErrorCode.PRODUCT_NOT_FOUND, "类目不存在"));
        vo.setCurrentSub(sub);

        List<CProduct> all = productMapper.selectList(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCategoryId, category.getId())
                .eq(CProduct::getStatus, 1)
                .orderByAsc(CProduct::getSort)
                .orderByAsc(CProduct::getId));

        List<CProduct> picked = all;
        boolean fallback = false;
        if (sub != null && !sub.isBlank()) {
            CSubCategory subCategory = subCategoryMapper.selectOne(new LambdaQueryWrapper<CSubCategory>()
                    .eq(CSubCategory::getCategoryId, category.getId())
                    .eq(CSubCategory::getCode, sub));
            List<CProduct> filtered = subCategory == null ? List.of()
                    : all.stream().filter(p -> p.getSubId().equals(subCategory.getId())).toList();
            if (filtered.isEmpty()) {
                // 该细分还在补货：返回整类，前端据此提示
                fallback = true;
            } else {
                picked = filtered;
            }
        }

        List<ProductVO> items = assembler.toVOList(picked);
        vo.setItems(items);
        vo.setFallback(fallback);
        return vo;
    }

    @Override
    public boolean exists(String code) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<CCategory>().eq(CCategory::getCode, code));
        return count != null && count > 0;
    }

    private CategoryVO copy(CategoryVO source) {
        CategoryVO vo = new CategoryVO();
        vo.setId(source.getId());
        vo.setName(source.getName());
        vo.setHint(source.getHint());
        vo.setGlyph(source.getGlyph());
        vo.setProductCount(source.getProductCount());
        vo.setSubs(source.getSubs());
        vo.setItems(List.of());
        vo.setFallback(false);
        return vo;
    }
}
