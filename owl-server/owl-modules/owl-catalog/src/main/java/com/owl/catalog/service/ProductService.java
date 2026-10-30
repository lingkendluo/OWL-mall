package com.owl.catalog.service;

import com.owl.catalog.api.ProductSnapshot;
import com.owl.catalog.vo.HomeVO;
import com.owl.catalog.vo.ProductDetailBundleVO;
import com.owl.catalog.vo.ProductVO;
import com.owl.common.api.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProductService {

    HomeVO home();

    PageResult<ProductVO> search(String keyword, String category, String sub, String sort, int page, int size);

    ProductDetailBundleVO detail(String code);

    List<ProductVO> featured(int limit);

    ProductSnapshot snapshot(String code);

    /** 批量取快照，购物车与下单都用这个，别在循环里查 */
    Map<String, ProductSnapshot> snapshots(Collection<String> codes);

    /** 购物车行存的是商品 id，这里按主键批量取 */
    Map<Long, ProductSnapshot> snapshotsByIds(Collection<Long> ids);
}
