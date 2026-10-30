package com.owl.catalog.service.impl;

import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CStockLog;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CStockLogMapper;
import com.owl.catalog.service.StockService;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final CProductMapper productMapper;
    private final CStockLogMapper stockLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long productId, int qty, String refNo) {
        if (qty <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "数量必须大于 0");
        }
        CProduct product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF);
        }
        int before = product.getStock() == null ? 0 : product.getStock();
        if (before < qty) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,
                    "「" + product.getName() + "」库存仅剩 " + before + " 件");
        }
        int rows = productMapper.deductStock(productId, qty, product.getVersion() == null ? 0 : product.getVersion());
        if (rows == 0) {
            // 并发抢购走到这里，属于正常业务失败，不是故障
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "「" + product.getName() + "」刚被人抢走了");
        }
        writeLog(productId, -qty, 1, refNo, before, before - qty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long productId, int qty, String refNo, int type) {
        if (qty <= 0) {
            return;
        }
        CProduct product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("回补库存时商品不存在 productId={} refNo={}", productId, refNo);
            return;
        }
        int before = product.getStock() == null ? 0 : product.getStock();
        int rows = productMapper.restoreStock(productId, qty);
        if (rows == 0) {
            log.warn("回补库存失败 productId={} refNo={}", productId, refNo);
            return;
        }
        writeLog(productId, qty, type, refNo, before, before + qty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long productId, int delta, String reason) {
        CProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        int before = product.getStock() == null ? 0 : product.getStock();
        int rows = productMapper.adjustStock(productId, delta);
        if (rows == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "库存不能为负，当前 " + before + " 件");
        }
        writeLog(productId, delta, 4, reason, before, before + delta);
    }

    private void writeLog(Long productId, int changeQty, int type, String refNo, int before, int after) {
        CStockLog log = new CStockLog();
        log.setProductId(productId);
        log.setChangeQty(changeQty);
        log.setType(type);
        log.setRefNo(refNo);
        log.setBeforeQty(before);
        log.setAfterQty(after);
        stockLogMapper.insert(log);
    }
}
