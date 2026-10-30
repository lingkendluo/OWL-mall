package com.owl.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.catalog.converter.ProductConverter;
import com.owl.catalog.dto.AdminProductRequest;
import com.owl.catalog.dto.ProductDetailDTO;
import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CProductDetail;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductDetailMapper;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.catalog.mapper.row.CategoryStatusCountRow;
import com.owl.catalog.service.ProductAdminService;
import com.owl.catalog.service.StockService;
import com.owl.catalog.vo.AdminProductVO;
import com.owl.catalog.vo.CategoryStatVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.PriceUtil;
import com.owl.infra.cache.CacheService;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

    private final CProductMapper productMapper;
    private final CProductDetailMapper detailMapper;
    private final CCategoryMapper categoryMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final StockService stockService;
    private final CacheService cache;
    private final KVStore kv;

    @Override
    public PageResult<AdminProductVO> list(String keyword, Integer status, String category, int page, int size) {
        // 大类索引传进来的是 code，先换成 id；查不到就直接返回空页，不要退化成「全部」
        Long categoryId = null;
        if (category != null && !category.isBlank()) {
            CCategory target = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                    .eq(CCategory::getCode, category));
            if (target == null) {
                return PageResult.of(List.of(), 0L, page, size);
            }
            categoryId = target.getId();
        }

        LambdaQueryWrapper<CProduct> wrapper = new LambdaQueryWrapper<CProduct>()
                .eq(status != null, CProduct::getStatus, status)
                .eq(categoryId != null, CProduct::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isBlank(), CProduct::getName, keyword)
                .orderByDesc(CProduct::getId);
        Page<CProduct> result = productMapper.selectPage(Page.of(page, size), wrapper);
        List<CProduct> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), page, size);
        }
        Map<Long, CCategory> categories = categoryMapper.selectBatchIds(
                        records.stream().map(CProduct::getCategoryId).distinct().toList()).stream()
                .collect(Collectors.toMap(CCategory::getId, Function.identity(), (a, b) -> a));
        Map<Long, CSubCategory> subs = subCategoryMapper.selectBatchIds(
                        records.stream().map(CProduct::getSubId).distinct().toList()).stream()
                .collect(Collectors.toMap(CSubCategory::getId, Function.identity(), (a, b) -> a));
        Map<Long, CProductDetail> details = detailMapper.selectBatchIds(
                        records.stream().map(CProduct::getId).toList()).stream()
                .collect(Collectors.toMap(CProductDetail::getProductId, Function.identity(), (a, b) -> a));

        List<AdminProductVO> list = records.stream().map(p -> {
            CCategory cat = categories.get(p.getCategoryId());
            CSubCategory sub = subs.get(p.getSubId());
            return ProductConverter.toAdminVO(p, details.get(p.getId()),
                    cat == null ? "" : cat.getCode(),
                    sub == null ? "" : sub.getCode());
        }).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public List<CategoryStatVO> categoryStats() {
        // 停用的大类不出现在索引里，和用户端 /c/categories 的口径保持一致
        List<CCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getStatus, 1)
                .orderByAsc(CCategory::getSort)
                .orderByAsc(CCategory::getId));
        if (categories.isEmpty()) {
            return List.of();
        }

        // [total, onSale] 两格累加，一次查询搞定，不给每个分类发 count
        Map<Long, int[]> agg = new LinkedHashMap<>();
        for (CategoryStatusCountRow row : productMapper.countByCategoryAndStatus()) {
            int[] slot = agg.computeIfAbsent(row.getCategoryId(), k -> new int[2]);
            int cnt = row.getCnt() == null ? 0 : row.getCnt();
            slot[0] += cnt;
            if (row.getStatus() != null && row.getStatus() == 1) {
                slot[1] += cnt;
            }
        }

        return categories.stream().map(c -> {
            int[] slot = agg.getOrDefault(c.getId(), new int[2]);
            CategoryStatVO vo = new CategoryStatVO();
            vo.setCode(c.getCode());
            vo.setName(c.getName());
            vo.setGlyph(c.getGlyph());
            vo.setTotal(slot[0]);
            vo.setOnSale(slot[1]);
            vo.setOffSale(slot[0] - slot[1]);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO create(AdminProductRequest request) {
        validatePrice(request);
        CCategory category = requireCategory(request.getCategoryCode());
        CSubCategory sub = requireSub(category.getId(), request.getSubCode());

        CProduct product = new CProduct();
        product.setCode(nextCode(category.getCode()));
        product.setCategoryId(category.getId());
        product.setSubId(sub.getId());
        apply(product, request);
        product.setSales(0);
        product.setVersion(0);
        product.setStatus(1);
        productMapper.insert(product);

        saveDetail(product.getId(), request.getDetail());
        evict();
        return toAdminVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO update(String code, AdminProductRequest request) {
        validatePrice(request);
        CProduct product = requireProduct(code);
        CCategory category = requireCategory(request.getCategoryCode());
        CSubCategory sub = requireSub(category.getId(), request.getSubCode());

        product.setCategoryId(category.getId());
        product.setSubId(sub.getId());
        apply(product, request);
        productMapper.updateById(product);

        saveDetail(product.getId(), request.getDetail());
        evict();
        return toAdminVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String code) {
        CProduct product = requireProduct(code);
        // 逻辑删除：订单行有快照，删商品不影响历史订单
        productMapper.deleteById(product.getId());
        evict();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO changeStatus(String code, int status) {
        CProduct product = requireProduct(code);
        product.setStatus(status);
        productMapper.updateById(product);
        evict();
        return toAdminVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO adjustStock(String code, int delta, String reason) {
        CProduct product = requireProduct(code);
        stockService.adjust(product.getId(), delta, reason);
        evict();
        return toAdminVO(requireProduct(code));
    }

    private void validatePrice(AdminProductRequest request) {
        long price = PriceUtil.toCent(request.getPrice());
        long list = PriceUtil.toCent(request.getList());
        if (price <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "售价必须大于 0");
        }
        if (price > list) {
            throw new BizException(ErrorCode.PARAM_ERROR, "售价不能高于划线价");
        }
    }

    private CCategory requireCategory(String code) {
        CCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getCode, code));
        if (category == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, "类目不存在");
        }
        return category;
    }

    private CSubCategory requireSub(Long categoryId, String code) {
        CSubCategory sub = subCategoryMapper.selectOne(new LambdaQueryWrapper<CSubCategory>()
                .eq(CSubCategory::getCategoryId, categoryId)
                .eq(CSubCategory::getCode, code));
        if (sub == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, "细分不存在");
        }
        return sub;
    }

    private CProduct requireProduct(String code) {
        CProduct product = productMapper.selectOne(new LambdaQueryWrapper<CProduct>().eq(CProduct::getCode, code));
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void apply(CProduct product, AdminProductRequest request) {
        product.setName(request.getName());
        product.setSpec(request.getSpec() == null ? "" : request.getSpec());
        product.setPriceCent(PriceUtil.toCent(request.getPrice()));
        product.setListCent(PriceUtil.toCent(request.getList()));
        product.setTag(request.getTag());
        product.setEmoji(request.getEmoji());
        product.setImage(request.getImage());
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setIsFeatured(Boolean.TRUE.equals(request.getIsFeatured()) ? 1 : 0);
    }

    private void saveDetail(Long productId, ProductDetailDTO dto) {
        CProductDetail detail = detailMapper.selectById(productId);
        boolean exists = detail != null;
        if (!exists) {
            detail = new CProductDetail();
            detail.setProductId(productId);
        }
        if (dto == null) {
            detail.setSlogan(detail.getSlogan() == null ? "" : detail.getSlogan());
            detail.setIntro(detail.getIntro() == null ? "" : detail.getIntro());
        } else {
            detail.setSlogan(dto.getSlogan() == null ? "" : dto.getSlogan());
            detail.setIntro(dto.getIntro() == null ? "" : dto.getIntro());
            detail.setFactsJson(dto.getFacts() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getFacts()));
            detail.setTipsJson(dto.getTips() == null ? List.of() : dto.getTips());
        }
        if (exists) {
            detailMapper.updateById(detail);
        } else {
            detailMapper.insert(detail);
        }
    }

    /** code = {categoryCode}-{序号}，生成过程加锁，防并发出现重复 code */
    private String nextCode(String categoryCode) {
        String lockKey = RedisKey.productCodeLock(categoryCode);
        boolean locked = false;
        try {
            for (int i = 0; i < 40 && !locked; i++) {
                locked = kv.setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
                if (!locked) {
                    Thread.sleep(50L);
                }
            }
            if (!locked) {
                throw new BizException(ErrorCode.TOO_FREQUENT, "商品编码生成冲突，请重试");
            }
            Integer max = productMapper.maxSeqOfCategory(categoryCode);
            int next = (max == null ? 0 : max) + 1;
            return categoryCode + "-" + next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.SYSTEM_ERROR, "商品编码生成被中断");
        } finally {
            if (locked) {
                kv.delete(lockKey);
            }
        }
    }

    private AdminProductVO toAdminVO(CProduct product) {
        CCategory category = categoryMapper.selectById(product.getCategoryId());
        CSubCategory sub = subCategoryMapper.selectById(product.getSubId());
        return ProductConverter.toAdminVO(product, detailMapper.selectById(product.getId()),
                category == null ? "" : category.getCode(),
                sub == null ? "" : sub.getCode());
    }

    /** 后台写操作很稀疏，直接清空本地缓存，比逐个 key 猜更不容易漏 */
    private void evict() {
        cache.evictAll();
    }
}
