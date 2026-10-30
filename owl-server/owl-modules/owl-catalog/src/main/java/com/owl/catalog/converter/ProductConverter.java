package com.owl.catalog.converter;

import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CProductDetail;
import com.owl.catalog.vo.AdminProductVO;
import com.owl.catalog.vo.ProductDetailVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.common.util.PriceUtil;

import java.util.LinkedHashMap;
import java.util.List;

public final class ProductConverter {

    private ProductConverter() {
    }

    public static ProductVO toVO(CProduct product, CProductDetail detail, String categoryCode, String subCode) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getCode());
        vo.setName(product.getName());
        vo.setSpec(product.getSpec());
        vo.setPrice(PriceUtil.toYuan(product.getPriceCent() == null ? 0L : product.getPriceCent()));
        vo.setList(PriceUtil.toYuan(product.getListCent() == null ? 0L : product.getListCent()));
        vo.setTag(product.getTag());
        vo.setEmoji(product.getEmoji());
        vo.setImage(product.getImage());
        vo.setCategory(categoryCode);
        vo.setSub(subCode);
        vo.setDetail(toDetailVO(detail));
        return vo;
    }

    public static ProductDetailVO toDetailVO(CProductDetail detail) {
        ProductDetailVO vo = new ProductDetailVO();
        if (detail == null) {
            vo.setSlogan("");
            vo.setIntro("");
            vo.setFacts(new LinkedHashMap<>());
            vo.setTips(List.of());
            return vo;
        }
        vo.setSlogan(detail.getSlogan());
        vo.setIntro(detail.getIntro());
        // 用 LinkedHashMap 兜底，保证前端拿到的是插入顺序而不是字母序
        vo.setFacts(detail.getFactsJson() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(detail.getFactsJson()));
        vo.setTips(detail.getTipsJson() == null ? List.of() : detail.getTipsJson());
        return vo;
    }

    public static AdminProductVO toAdminVO(CProduct product, CProductDetail detail, String categoryCode, String subCode) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(product.getCode());
        vo.setName(product.getName());
        vo.setCategoryCode(categoryCode);
        vo.setSubCode(subCode);
        vo.setSpec(product.getSpec());
        vo.setPrice(PriceUtil.toYuan(product.getPriceCent() == null ? 0L : product.getPriceCent()));
        vo.setList(PriceUtil.toYuan(product.getListCent() == null ? 0L : product.getListCent()));
        vo.setTag(product.getTag());
        vo.setEmoji(product.getEmoji());
        vo.setImage(product.getImage());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());
        vo.setIsFeatured(product.getIsFeatured() != null && product.getIsFeatured() == 1);
        vo.setDetail(toDetailVO(detail));
        return vo;
    }
}
