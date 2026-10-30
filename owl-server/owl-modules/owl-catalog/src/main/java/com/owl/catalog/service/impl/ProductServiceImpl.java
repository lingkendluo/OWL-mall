package com.owl.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.catalog.api.ProductSnapshot;
import com.owl.catalog.converter.ProductAssembler;
import com.owl.catalog.converter.ProductConverter;
import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CProductDetail;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductDetailMapper;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.catalog.service.CategoryService;
import com.owl.catalog.service.ProductService;
import com.owl.catalog.vo.BrandVO;
import com.owl.catalog.vo.CategoryVO;
import com.owl.catalog.vo.HomeVO;
import com.owl.catalog.vo.ProductDetailBundleVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.PriceUtil;
import com.owl.infra.cache.CacheService;
import com.owl.infra.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    static final String CACHE_HOME = "home";
    static final String CACHE_FEATURED = "featured";
    static final String CACHE_PRODUCT = "product:";
    private static final Duration TTL_HOME = Duration.ofMinutes(5);

    private final CProductMapper productMapper;
    private final CProductDetailMapper detailMapper;
    private final CCategoryMapper categoryMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final CategoryService categoryService;
    private final ProductAssembler assembler;
    private final CacheService cache;
    private final ShippingProperties shipping;

    @Override
    public HomeVO home() {
        return cache.getOrLoad(CACHE_HOME, TTL_HOME, () -> {
            HomeVO vo = new HomeVO();
            List<CategoryVO> categories = categoryService.listAll();
            vo.setCategories(categories);

            BrandVO brand = new BrandVO();
            brand.setName("OWL 商城");
            int subs = categories.stream().mapToInt(c -> c.getSubs().size()).sum();
            brand.setDesc(categories.size() + " 个大类 · " + subs + " 个细分 · 最快 1 小时送达");
            vo.setBrand(brand);

            vo.setFeatured(featured(12));
            vo.setFreeShippingAt(PriceUtil.toYuan(shipping.getFreeThresholdCent()));
            vo.setShippingText("满 ¥" + PriceUtil.toYuan(shipping.getFreeThresholdCent()).toPlainString()
                    + " 免运费 · 最快 1 小时送达");
            return vo;
        });
    }

    @Override
    public PageResult<ProductVO> search(String keyword, String category, String sub, String sort, int page, int size) {
        String kw = keyword == null ? null : keyword.trim();
        Long categoryId = null;
        if (category != null && !category.isBlank()) {
            CCategory cat = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                    .eq(CCategory::getCode, category));
            if (cat == null) {
                return PageResult.empty(page, size);
            }
            categoryId = cat.getId();
        }

        LambdaQueryWrapper<CProduct> wrapper = new LambdaQueryWrapper<CProduct>().eq(CProduct::getStatus, 1);

        if (categoryId != null) {
            wrapper.eq(CProduct::getCategoryId, categoryId);
        }
        if (sub != null && !sub.isBlank()) {
            List<Long> subIds = subCategoryMapper.selectList(new LambdaQueryWrapper<CSubCategory>()
                            .eq(CSubCategory::getCode, sub)
                            .eq(categoryId != null, CSubCategory::getCategoryId, categoryId))
                    .stream().map(CSubCategory::getId).toList();
            if (subIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(CProduct::getSubId, subIds);
        }

        if (kw != null && !kw.isBlank()) {
            // 前端 searchHits 同时匹配商品名、规格、类目名/副文案、细分名，这里一条 SQL 覆盖
            List<Long> catIds = categoryMapper.selectList(new LambdaQueryWrapper<CCategory>()
                            .like(CCategory::getName, kw).or().like(CCategory::getHint, kw))
                    .stream().map(CCategory::getId).toList();
            List<Long> subIds = subCategoryMapper.selectList(new LambdaQueryWrapper<CSubCategory>()
                            .like(CSubCategory::getName, kw))
                    .stream().map(CSubCategory::getId).toList();
            wrapper.and(w -> {
                w.like(CProduct::getName, kw).or().like(CProduct::getSpec, kw);
                if (!catIds.isEmpty()) {
                    w.or().in(CProduct::getCategoryId, catIds);
                }
                if (!subIds.isEmpty()) {
                    w.or().in(CProduct::getSubId, subIds);
                }
            });
        }

        switch (sort == null ? "default" : sort) {
            case "price_asc" -> wrapper.orderByAsc(CProduct::getPriceCent);
            case "price_desc" -> wrapper.orderByDesc(CProduct::getPriceCent);
            case "sales_desc" -> wrapper.orderByDesc(CProduct::getSales);
            default -> wrapper.orderByAsc(CProduct::getSort).orderByAsc(CProduct::getId);
        }

        Page<CProduct> result = productMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(assembler.toVOList(result.getRecords()), result.getTotal(), page, size);
    }

    @Override
    public ProductDetailBundleVO detail(String code) {
        ProductVO base = cache.getOrLoad(CACHE_PRODUCT + code, TTL_HOME, () -> loadProductVO(code));
        ProductDetailBundleVO bundle = new ProductDetailBundleVO();
        copyInto(bundle, base);
        bundle.setSiblings(siblings(code));
        return bundle;
    }

    private ProductVO loadProductVO(String code) {
        CProduct product = productMapper.selectOne(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCode, code)
                .eq(CProduct::getStatus, 1));
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return assembler.toVO(product);
    }

    /** 同细分优先，不足用同类补，最多 4 件，排除自己且不重复 */
    private List<ProductVO> siblings(String code) {
        CProduct self = productMapper.selectOne(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCode, code)
                .eq(CProduct::getStatus, 1));
        if (self == null) {
            return List.of();
        }
        List<CProduct> pool = productMapper.selectList(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCategoryId, self.getCategoryId())
                .ne(CProduct::getId, self.getId())
                .eq(CProduct::getStatus, 1)
                .orderByAsc(CProduct::getSort)
                .orderByAsc(CProduct::getId));
        List<CProduct> ordered = new ArrayList<>();
        pool.stream().filter(p -> p.getSubId().equals(self.getSubId())).forEach(ordered::add);
        pool.stream().filter(p -> !p.getSubId().equals(self.getSubId())).forEach(ordered::add);
        return assembler.toVOList(ordered.stream().limit(4).toList());
    }

    @Override
    public List<ProductVO> featured(int limit) {
        int size = limit <= 0 ? 12 : Math.min(limit, 50);
        List<ProductVO> cached = cache.getOrLoad(CACHE_FEATURED + size, TTL_HOME, () -> {
            List<CProduct> list = productMapper.selectList(new LambdaQueryWrapper<CProduct>()
                    .eq(CProduct::getIsFeatured, 1)
                    .eq(CProduct::getStatus, 1)
                    .orderByAsc(CProduct::getSort)
                    .orderByAsc(CProduct::getId)
                    .last("LIMIT " + size));
            return assembler.toVOList(list);
        });
        return cached == null ? List.of() : cached;
    }

    @Override
    public ProductSnapshot snapshot(String code) {
        CProduct product = productMapper.selectOne(new LambdaQueryWrapper<CProduct>().eq(CProduct::getCode, code));
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return toSnapshot(product, subCategoryMapper.selectById(product.getSubId()));
    }

    @Override
    public Map<String, ProductSnapshot> snapshots(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }
        List<CProduct> products = productMapper.selectList(new LambdaQueryWrapper<CProduct>()
                .in(CProduct::getCode, codes));
        if (products.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> subCodes = assembler.loadSubCodes(products.stream().map(CProduct::getSubId).toList());
        Map<String, ProductSnapshot> result = new LinkedHashMap<>();
        for (CProduct product : products) {
            result.put(product.getCode(), toSnapshot(product, subCodes.get(product.getSubId())));
        }
        return result;
    }

    @Override
    public Map<Long, ProductSnapshot> snapshotsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<CProduct> products = productMapper.selectBatchIds(ids);
        if (products.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> subCodes = assembler.loadSubCodes(products.stream().map(CProduct::getSubId).toList());
        Map<Long, ProductSnapshot> result = new LinkedHashMap<>();
        for (CProduct product : products) {
            result.put(product.getId(), toSnapshot(product, subCodes.get(product.getSubId())));
        }
        return result;
    }

    private ProductSnapshot toSnapshot(CProduct product, CSubCategory sub) {        return toSnapshot(product, sub == null ? "" : sub.getCode());
    }

    private ProductSnapshot toSnapshot(CProduct product, String subCode) {
        return new ProductSnapshot(
                product.getId(),
                product.getCode(),
                product.getCategoryId(),
                product.getSubId(),
                product.getName(),
                product.getSpec(),
                product.getImage(),
                product.getEmoji(),
                product.getPriceCent() == null ? 0L : product.getPriceCent(),
                product.getListCent() == null ? 0L : product.getListCent(),
                product.getStock() == null ? 0 : product.getStock(),
                product.getStatus() == null ? 0 : product.getStatus(),
                product.getVersion() == null ? 0 : product.getVersion(),
                subCode == null ? "" : subCode);
    }

    private void copyInto(ProductDetailBundleVO target, ProductVO source) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setSpec(source.getSpec());
        target.setPrice(source.getPrice());
        target.setList(source.getList());
        target.setTag(source.getTag());
        target.setEmoji(source.getEmoji());
        target.setImage(source.getImage());
        target.setCategory(source.getCategory());
        target.setSub(source.getSub());
        target.setDetail(source.getDetail());
    }
}
