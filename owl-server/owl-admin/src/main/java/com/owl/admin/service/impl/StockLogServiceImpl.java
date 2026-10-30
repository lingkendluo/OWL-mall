package com.owl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.admin.service.StockLogService;
import com.owl.admin.vo.StockLogVO;
import com.owl.catalog.entity.CProduct;
import com.owl.catalog.entity.CStockLog;
import com.owl.catalog.mapper.CProductMapper;
import com.owl.catalog.mapper.CStockLogMapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.TimeTextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CStockLogMapper stockLogMapper;
    private final CProductMapper productMapper;

    @Override
    public PageResult<StockLogVO> listByProduct(String code, int page, int size) {
        CProduct product = productMapper.selectOne(new LambdaQueryWrapper<CProduct>()
                .eq(CProduct::getCode, code));
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Page<CStockLog> result = stockLogMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<CStockLog>()
                        .eq(CStockLog::getProductId, product.getId())
                        .orderByDesc(CStockLog::getId));
        List<StockLogVO> list = result.getRecords().stream().map(row -> {
            StockLogVO vo = new StockLogVO();
            vo.setId(String.valueOf(row.getId()));
            vo.setChangeQty(row.getChangeQty());
            vo.setType(row.getType());
            vo.setTypeText(typeText(row.getType()));
            vo.setBeforeQty(row.getBeforeQty());
            vo.setAfterQty(row.getAfterQty());
            vo.setRefNo(row.getRefNo());
            vo.setCreatedAt(millis(row.getCreatedAt()));
            vo.setTime(TimeTextUtil.format(row.getCreatedAt()));
            return vo;
        }).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    private String typeText(Integer type) {
        if (type == null) {
            return "调整";
        }
        return switch (type) {
            case 1 -> "下单扣减";
            case 2 -> "取消回补";
            case 3 -> "采购入库";
            case 4 -> "后台调整";
            default -> "调整";
        };
    }

    private Long millis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
