package com.owl.admin.service;

import com.owl.admin.vo.StockLogVO;
import com.owl.common.api.PageResult;

public interface StockLogService {

    /**
     * 某个商品的库存流水，按时间倒序。
     *
     * @param code 商品对外 code，如 snacks-1
     */
    PageResult<StockLogVO> listByProduct(String code, int page, int size);
}
