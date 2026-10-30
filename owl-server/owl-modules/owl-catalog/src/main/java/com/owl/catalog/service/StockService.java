package com.owl.catalog.service;

public interface StockService {

    /** 下单扣库存：乐观锁 CAS，抢不到抛 30003 */
    void deduct(Long productId, int qty, String refNo);

    /**
     * 回补库存
     *
     * @param type 2 取消回补 / 3 入库
     */
    void restore(Long productId, int qty, String refNo, int type);

    /** 后台调整，delta 正负皆可，结果不能为负 */
    void adjust(Long productId, int delta, String reason);
}
