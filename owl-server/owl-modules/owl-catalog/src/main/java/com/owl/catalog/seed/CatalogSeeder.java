package com.owl.catalog.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.catalog.entity.CCategory;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CProductDetail;
import com.owl.catalog.entity.CSubCategory;
import com.owl.catalog.mapper.CCategoryMapper;
import com.owl.catalog.mapper.CProductDetailMapper;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CSubCategoryMapper;
import com.owl.infra.cache.CacheService;
import com.owl.infra.config.SeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品种子导入。幂等：按 code upsert，已存在就跳过，不覆盖后台改过的数据。
 * 数据源是前端 catalog.ts 导出的 seed/catalog.json（15 类 / 120 细分 / 90 商品）。
 */
@Slf4j
@Order(10)
@Component
@RequiredArgsConstructor
public class CatalogSeeder implements ApplicationRunner {

    /** 首页推荐位：前端 FEATURED_IDS 那 12 件，跨类目挑的，不能用"前 12 条"代替 */
    private static final Set<String> FEATURED_CODES = Set.of(
            "fruit-3", "meat-1", "appliance-6", "phone-6", "beauty-2", "snacks-1",
            "bedding-2", "furniture-2", "jewelry-2", "daily-1", "veg-5", "womenswear-3");

    private static final int INIT_STOCK = 200;

    private final CCategoryMapper categoryMapper;
    private final CSubCategoryMapper subCategoryMapper;
    private final CProductMapper productMapper;
    private final CProductDetailMapper detailMapper;
    private final CacheService cache;
    private final SeedProperties seedProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        if (!seedProperties.isEnabled()) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("seed/catalog.json");
        if (!resource.exists()) {
            log.warn("seed/catalog.json 不存在，跳过商品初始化");
            return;
        }

        JsonNode root;
        try (InputStream in = resource.getInputStream()) {
            root = new ObjectMapper().readTree(in);
        }
        if (root == null || !root.isArray()) {
            log.warn("seed/catalog.json 格式不对，跳过商品初始化");
            return;
        }

        int categoryCount = 0;
        int subCount = 0;
        int productCount = 0;
        Map<String, Long> subIdIndex = new HashMap<>();
        Map<String, Long> categoryIdIndex = new HashMap<>();

        for (JsonNode node : root) {
            String categoryCode = node.path("code").asText();
            Long categoryId = upsertCategory(node, categoryCode);
            categoryIdIndex.put(categoryCode, categoryId);
            if (categoryId != null) {
                categoryCount++;
            }

            for (JsonNode sub : node.path("subs")) {
                String subCode = sub.path("code").asText();
                Long subId = upsertSub(categoryId, sub, subCode);
                subIdIndex.put(categoryCode + "/" + subCode, subId);
                if (subId != null) {
                    subCount++;
                }
            }

            for (JsonNode product : node.path("products")) {
                String productCode = product.path("code").asText();
                Long subId = subIdIndex.get(categoryCode + "/" + product.path("sub").asText());
                if (subId == null) {
                    log.warn("商品 {} 的细分不存在，跳过", productCode);
                    continue;
                }
                if (upsertProduct(categoryId, subId, product, productCode)) {
                    productCount++;
                }
            }
        }

        cache.evictAll();
        log.info("catalog seeded: {} categories / {} subs / {} products", categoryCount, subCount, productCount);
    }

    private Long upsertCategory(JsonNode node, String code) {
        CCategory exists = categoryMapper.selectOne(new LambdaQueryWrapper<CCategory>()
                .eq(CCategory::getCode, code));
        if (exists != null) {
            return exists.getId();
        }
        CCategory category = new CCategory();
        category.setCode(code);
        category.setName(node.path("name").asText());
        category.setHint(node.path("hint").asText(""));
        category.setGlyph(node.path("glyph").asText(null));
        category.setSort(node.path("sort").asInt(0));
        category.setStatus(1);
        categoryMapper.insert(category);
        return category.getId();
    }

    private Long upsertSub(Long categoryId, JsonNode node, String code) {
        CSubCategory exists = subCategoryMapper.selectOne(new LambdaQueryWrapper<CSubCategory>()
                .eq(CSubCategory::getCategoryId, categoryId)
                .eq(CSubCategory::getCode, code));
        if (exists != null) {
            return exists.getId();
        }
        CSubCategory sub = new CSubCategory();
        sub.setCategoryId(categoryId);
        sub.setCode(code);
        sub.setName(node.path("name").asText());
        sub.setSort(node.path("sort").asInt(0));
        sub.setStatus(1);
        subCategoryMapper.insert(sub);
        return sub.getId();
    }

    private boolean upsertProduct(Long categoryId, Long subId, JsonNode node, String code) {
        CProduct product = productMapper.selectOne(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCode, code));
        if (product != null) {
            return false;
        }
        product = new CProduct();
        product.setCode(code);
        product.setCategoryId(categoryId);
        product.setSubId(subId);
        product.setName(node.path("name").asText());
        product.setSpec(node.path("spec").asText(""));
        product.setPriceCent(toCent(node.path("price")));
        product.setListCent(toCent(node.path("list")));
        product.setTag(node.path("tag").asText(null));
        product.setEmoji(node.path("emoji").asText(null));
        product.setImage("/static/products/" + code + ".jpg");
        product.setStock(INIT_STOCK);
        // 固定种子伪随机，保证每次重建库销量一样，方便对比
        product.setSales(Math.floorMod(code.hashCode(), 51));
        product.setSort(node.path("sort").asInt(0));
        product.setIsFeatured(FEATURED_CODES.contains(code) ? 1 : 0);
        product.setStatus(1);
        product.setVersion(0);
        productMapper.insert(product);

        JsonNode detailNode = node.path("detail");
        CProductDetail detail = new CProductDetail();
        detail.setProductId(product.getId());
        detail.setSlogan(detailNode.path("slogan").asText(""));
        detail.setIntro(detailNode.path("intro").asText(""));
        detail.setFactsJson(readFacts(detailNode.path("facts")));
        detail.setTipsJson(readTips(detailNode.path("tips")));
        detailMapper.insert(detail);
        return true;
    }

    private Map<String, String> readFacts(JsonNode node) {
        Map<String, String> facts = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(field -> facts.put(field, node.path(field).asText("")));
        }
        return facts;
    }

    private List<String> readTips(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> tips = new java.util.ArrayList<>();
        node.forEach(item -> tips.add(item.asText("")));
        return tips;
    }

    /** catalog.json 里的价格是元，落库转成分 */
    private long toCent(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0L;
        }
        return new BigDecimal(node.asText("0")).movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}
