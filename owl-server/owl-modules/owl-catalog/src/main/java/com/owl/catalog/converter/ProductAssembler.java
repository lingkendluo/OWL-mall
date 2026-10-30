package com.owl.catalog.converter;

import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CProductDetail;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductDetailMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.catalog.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 把商品实体装配成 VO。详情、细分 code、大类 code 都是批量查，避免列表里 N+1。
 */
@Component
@RequiredArgsConstructor
public class ProductAssembler {

    private final CProductDetailMapper detailMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final CCategoryMapper categoryMapper;

    public List<ProductVO> toVOList(List<CProduct> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        Map<Long, CProductDetail> details = loadDetails(products.stream().map(CProduct::getId).toList());
        Map<Long, String> subCodes = loadSubCodes(products.stream().map(CProduct::getSubId).toList());
        Map<Long, String> categoryCodes = loadCategoryCodes(
                products.stream().map(CProduct::getCategoryId).toList());
        return products.stream()
                .map(p -> ProductConverter.toVO(p, details.get(p.getId()),
                        categoryCodes.getOrDefault(p.getCategoryId(), ""),
                        subCodes.getOrDefault(p.getSubId(), "")))
                .toList();
    }

    public ProductVO toVO(CProduct product) {
        CProductDetail detail = detailMapper.selectById(product.getId());
        CSubCategory sub = subCategoryMapper.selectById(product.getSubId());
        CCategory category = categoryMapper.selectById(product.getCategoryId());
        return ProductConverter.toVO(product, detail,
                category == null ? "" : category.getCode(),
                sub == null ? "" : sub.getCode());
    }

    public Map<Long, CProductDetail> loadDetails(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return detailMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(CProductDetail::getProductId, Function.identity(), (a, b) -> a));
    }

    public Map<Long, String> loadSubCodes(Collection<Long> subIds) {
        if (subIds == null || subIds.isEmpty()) {
            return Map.of();
        }
        return subCategoryMapper.selectBatchIds(subIds).stream()
                .collect(Collectors.toMap(CSubCategory::getId, CSubCategory::getCode, (a, b) -> a));
    }

    public Map<Long, String> loadCategoryCodes(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CCategory::getId, CCategory::getCode, (a, b) -> a));
    }
}
