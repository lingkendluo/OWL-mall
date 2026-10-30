package com.owl.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.catalog.api.ProductSnapshot;
import com.owl.catalog.service.ProductService;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.PriceUtil;
import com.owl.infra.config.TradeProperties;
import com.owl.trade.dto.CartAddRequest;
import com.owl.trade.dto.CartCheckRequest;
import com.owl.trade.dto.CartMergeLineDTO;
import com.owl.trade.dto.CartMergeRequest;
import com.owl.trade.dto.CartUpdateRequest;
import com.owl.trade.entity.TCartItem;
import com.owl.trade.mapper.TCartItemMapper;
import com.owl.trade.service.CartService;
import com.owl.trade.service.PriceCalculator;
import com.owl.trade.vo.CartLineVO;
import com.owl.trade.vo.CartMergeVO;
import com.owl.trade.vo.CartSummaryVO;
import com.owl.trade.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final TCartItemMapper cartMapper;
    private final ProductService productService;
    private final PriceCalculator priceCalculator;
    private final TradeProperties tradeProperties;

    @Override
    public CartVO get(Long userId) {
        List<TCartItem> rows = cartMapper.selectList(new LambdaQueryWrapper<TCartItem>()
                .eq(TCartItem::getUserId, userId)
                .orderByDesc(TCartItem::getUpdatedAt)
                .orderByDesc(TCartItem::getId));
        return build(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO add(Long userId, CartAddRequest request) {
        ProductSnapshot snapshot = productService.snapshot(request.getProductId());
        if (snapshot == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!snapshot.onSale()) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF);
        }
        if (snapshot.stock() <= 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        int qty = clampQty(request.getQty());
        TCartItem exists = findRow(userId, snapshot.id());
        if (exists == null) {
            TCartItem row = new TCartItem();
            row.setUserId(userId);
            row.setProductId(snapshot.id());
            row.setQty(qty);
            // 新加的行默认勾选，对齐前端 add()
            row.setChecked(1);
            cartMapper.insert(row);
        } else {
            cartMapper.update(null, new LambdaUpdateWrapper<TCartItem>()
                    .eq(TCartItem::getId, exists.getId())
                    .set(TCartItem::getQty, Math.min(maxQty(), exists.getQty() + qty))
                    .set(TCartItem::getChecked, 1));
        }
        return get(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO updateLine(Long userId, String productId, CartUpdateRequest request) {
        TCartItem row = requireRow(userId, productId);
        LambdaUpdateWrapper<TCartItem> update = new LambdaUpdateWrapper<TCartItem>()
                .eq(TCartItem::getId, row.getId());
        if (request.getQty() != null) {
            // 越界截断，不报错
            update.set(TCartItem::getQty, clampQty(request.getQty()));
        }
        if (request.getChecked() != null) {
            update.set(TCartItem::getChecked, request.getChecked() ? 1 : 0);
        }
        cartMapper.update(null, update);
        return get(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO remove(Long userId, String productId) {
        ProductSnapshot snapshot = productService.snapshot(productId);
        if (snapshot != null) {
            cartMapper.delete(new LambdaQueryWrapper<TCartItem>()
                    .eq(TCartItem::getUserId, userId)
                    .eq(TCartItem::getProductId, snapshot.id()));
        }
        return get(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO check(Long userId, CartCheckRequest request) {
        boolean checked = Boolean.TRUE.equals(request.getChecked());
        LambdaUpdateWrapper<TCartItem> update = new LambdaUpdateWrapper<TCartItem>()
                .eq(TCartItem::getUserId, userId)
                .set(TCartItem::getChecked, checked ? 1 : 0);
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            Map<String, ProductSnapshot> snapshots = productService.snapshots(request.getIds());
            List<Long> productIds = snapshots.values().stream().map(ProductSnapshot::id).toList();
            if (productIds.isEmpty()) {
                return get(userId);
            }
            update.in(TCartItem::getProductId, productIds);
        }
        cartMapper.update(null, update);
        return get(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO removeChecked(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<TCartItem>()
                .eq(TCartItem::getUserId, userId)
                .eq(TCartItem::getChecked, 1));
        return get(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<TCartItem>().eq(TCartItem::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartMergeVO merge(Long userId, CartMergeRequest request) {
        List<String> skipped = new ArrayList<>();
        if (request.getLines() != null) {
            for (CartMergeLineDTO line : request.getLines()) {
                if (line.getProductId() == null || line.getProductId().isBlank()) {
                    continue;
                }
                ProductSnapshot snapshot;
                try {
                    snapshot = productService.snapshot(line.getProductId());
                } catch (BizException e) {
                    skipped.add(line.getProductId());
                    continue;
                }
                // 下架的商品跳过并回报，前端据此清掉本地那几行
                if (snapshot == null || !snapshot.onSale()) {
                    skipped.add(line.getProductId());
                    continue;
                }
                int qty = clampQty(line.getQty());
                TCartItem exists = findRow(userId, snapshot.id());
                if (exists == null) {
                    TCartItem row = new TCartItem();
                    row.setUserId(userId);
                    row.setProductId(snapshot.id());
                    row.setQty(qty);
                    row.setChecked(1);
                    cartMapper.insert(row);
                } else {
                    cartMapper.update(null, new LambdaUpdateWrapper<TCartItem>()
                            .eq(TCartItem::getId, exists.getId())
                            .set(TCartItem::getQty, Math.min(maxQty(), exists.getQty() + qty)));
                }
            }
        }
        CartVO cart = get(userId);
        CartMergeVO vo = new CartMergeVO();
        vo.setLines(cart.getLines());
        vo.setSummary(cart.getSummary());
        vo.setSkipped(skipped);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLines(Long userId, Collection<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return;
        }
        Map<String, ProductSnapshot> snapshots = productService.snapshots(productCodes);
        List<Long> productIds = snapshots.values().stream().map(ProductSnapshot::id).toList();
        if (productIds.isEmpty()) {
            return;
        }
        cartMapper.delete(new LambdaQueryWrapper<TCartItem>()
                .eq(TCartItem::getUserId, userId)
                .in(TCartItem::getProductId, productIds));
    }

    @Override
    public void deleteAllOfUser(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<TCartItem>().eq(TCartItem::getUserId, userId));
    }

    @Override
    public List<String> codesOf(Long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<TCartItem>().eq(TCartItem::getUserId, userId))
                .stream().map(TCartItem::getProductId).map(String::valueOf).toList();
    }

    private TCartItem findRow(Long userId, Long productId) {
        return cartMapper.selectOne(new LambdaQueryWrapper<TCartItem>()
                .eq(TCartItem::getUserId, userId)
                .eq(TCartItem::getProductId, productId));
    }

    private TCartItem requireRow(Long userId, String productCode) {
        ProductSnapshot snapshot = productService.snapshot(productCode);
        TCartItem row = snapshot == null ? null : findRow(userId, snapshot.id());
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "购物车中没有该商品");
        }
        return row;
    }

    private CartVO build(List<TCartItem> rows) {
        CartVO vo = new CartVO();
        if (rows == null || rows.isEmpty()) {
            vo.setLines(List.of());
            vo.setSummary(emptySummary());
            return vo;
        }
        // 一次批量取商品，禁止在循环里查库
        Map<Long, ProductSnapshot> byId = productService.snapshotsByIds(
                rows.stream().map(TCartItem::getProductId).toList());

        List<CartLineVO> lines = new ArrayList<>(rows.size());
        List<PriceCalculator.PriceLine> allLines = new ArrayList<>();
        List<PriceCalculator.PriceLine> checkedLines = new ArrayList<>();

        for (TCartItem row : rows) {
            ProductSnapshot snapshot = byId.get(row.getProductId());
            CartLineVO line = new CartLineVO();
            line.setQty(row.getQty());
            line.setChecked(row.getChecked() != null && row.getChecked() == 1);
            boolean invalid = snapshot == null || !snapshot.onSale();
            line.setInvalid(invalid);
            if (snapshot != null) {
                line.setId(snapshot.code());
                line.setName(snapshot.name());
                line.setSpec(snapshot.spec());
                line.setEmoji(snapshot.emoji());
                line.setImage(snapshot.image());
                line.setPrice(PriceUtil.toYuan(snapshot.priceCent()));
                line.setList(PriceUtil.toYuan(snapshot.listCent()));
                if (!invalid) {
                    PriceCalculator.PriceLine priceLine = new PriceCalculator.PriceLine(
                            snapshot.code(), snapshot.priceCent(), snapshot.listCent(), row.getQty());
                    allLines.add(priceLine);
                    if (line.getChecked()) {
                        checkedLines.add(priceLine);
                    }
                }
            } else {
                line.setId(String.valueOf(row.getProductId()));
                line.setName("商品已下架");
                line.setSpec("");
                line.setPrice(PriceUtil.toYuan(0));
                line.setList(PriceUtil.toYuan(0));
            }
            lines.add(line);
        }

        PriceCalculator.PriceResult all = priceCalculator.calculateGoods(allLines);
        PriceCalculator.PriceResult checked = priceCalculator.calculateGoods(checkedLines);

        CartSummaryVO summary = new CartSummaryVO();
        summary.setCount(lines.stream().filter(l -> !l.getInvalid()).mapToInt(CartLineVO::getQty).sum());
        summary.setAmount(PriceUtil.toYuan(all.goodsCent()));
        summary.setSaved(PriceUtil.toYuan(all.savedCent()));

        int checkedCount = checkedLines.stream().mapToInt(PriceCalculator.PriceLine::qty).sum();
        long checkedAmount = checked.goodsCent();
        summary.setCheckedCount(checkedCount);
        summary.setCheckedAmount(PriceUtil.toYuan(checkedAmount));
        summary.setAllChecked(!lines.isEmpty() && lines.stream().allMatch(CartLineVO::getChecked));

        long threshold = priceCalculator.freeThresholdCent();
        boolean free = checkedCount > 0 && checkedAmount >= threshold;
        summary.setIsFreeShipping(free);
        summary.setToFreeShipping(PriceUtil.toYuan(Math.max(0L, threshold - checkedAmount)));
        summary.setShippingRate(checkedCount == 0 ? 0 : (int) Math.min(100L, checkedAmount * 100 / threshold));
        summary.setFreeShippingAt(PriceUtil.toYuan(threshold));
        summary.setShipFee(PriceUtil.toYuan(checkedCount == 0 ? 0L : (free ? 0L : priceCalculator.shipFeeCent())));

        vo.setLines(lines);
        vo.setSummary(summary);
        return vo;
    }

    private CartSummaryVO emptySummary() {
        CartSummaryVO summary = new CartSummaryVO();
        summary.setCount(0);
        summary.setAmount(PriceUtil.toYuan(0));
        summary.setSaved(PriceUtil.toYuan(0));
        summary.setCheckedCount(0);
        summary.setCheckedAmount(PriceUtil.toYuan(0));
        summary.setAllChecked(false);
        summary.setIsFreeShipping(false);
        summary.setToFreeShipping(PriceUtil.toYuan(priceCalculator.freeThresholdCent()));
        summary.setShippingRate(0);
        summary.setFreeShippingAt(PriceUtil.toYuan(priceCalculator.freeThresholdCent()));
        summary.setShipFee(PriceUtil.toYuan(0));
        return summary;
    }

    private int clampQty(Integer qty) {
        int value = qty == null ? 1 : qty;
        return Math.max(1, Math.min(maxQty(), value));
    }

    private int maxQty() {
        return tradeProperties.getMaxQty();
    }
}
