package com.owl.trade.service.impl;

import com.owl.auth.service.AddressService;
import com.owl.auth.service.CouponService;
import com.owl.auth.vo.AddressVO;
import com.owl.auth.vo.CouponBriefVO;
import com.owl.catalog.api.ProductSnapshot;
import com.owl.catalog.service.ProductService;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.PriceUtil;
import com.owl.trade.dto.OrderLineDTO;
import com.owl.trade.dto.PreviewRequest;
import com.owl.trade.service.CheckoutService;
import com.owl.trade.service.PriceCalculator;
import com.owl.trade.vo.AmountVO;
import com.owl.trade.vo.PreviewItemVO;
import com.owl.trade.vo.PreviewVO;
import com.owl.trade.vo.ShippingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final ProductService productService;
    private final CouponService couponService;
    private final AddressService addressService;
    private final PriceCalculator priceCalculator;

    @Override
    public PreviewVO preview(Long userId, PreviewRequest request) {
        List<OrderLineDTO> lines = request.getLines();
        if (lines == null || lines.isEmpty()) {
            throw new BizException(ErrorCode.CART_EMPTY, "请选择要结算的商品");
        }

        Map<String, ProductSnapshot> snapshots = productService.snapshots(
                lines.stream().map(OrderLineDTO::getProductId).toList());

        List<PreviewItemVO> items = new ArrayList<>();
        List<PriceCalculator.PriceLine> priceLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (OrderLineDTO line : lines) {
            ProductSnapshot snapshot = snapshots.get(line.getProductId());
            if (snapshot == null) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (!snapshot.onSale()) {
                throw new BizException(ErrorCode.PRODUCT_OFF_SHELF, "「" + snapshot.name() + "」已下架");
            }
            int qty = line.getQty() == null || line.getQty() < 1 ? 1 : line.getQty();

            PreviewItemVO item = new PreviewItemVO();
            item.setId(snapshot.code());
            item.setName(snapshot.name());
            item.setSpec(snapshot.spec());
            item.setImage(snapshot.image());
            item.setPrice(PriceUtil.toYuan(snapshot.priceCent()));
            item.setList(PriceUtil.toYuan(snapshot.listCent()));
            item.setQty(qty);
            item.setTotal(PriceUtil.toYuan(snapshot.priceCent() * qty));
            items.add(item);

            priceLines.add(new PriceCalculator.PriceLine(
                    snapshot.code(), snapshot.priceCent(), snapshot.listCent(), qty));

            // 库存不足只在 warnings 提示，下单时才真正失败
            if (snapshot.stock() < qty) {
                warnings.add("「" + snapshot.name() + "」库存仅剩 " + snapshot.stock() + " 件");
            }
        }

        long goodsCent = priceLines.stream().mapToLong(l -> l.priceCent() * l.qty()).sum();
        Long couponId = parseId(request.getCouponId());
        CouponService.CouponCheck coupon = couponService.check(couponId, userId, goodsCent);
        PriceCalculator.PriceResult result = priceCalculator.calculate(priceLines, coupon.discountCent());

        PreviewVO vo = new PreviewVO();
        vo.setItems(items);
        vo.setAmounts(toAmount(result));
        vo.setShipping(toShipping(result));
        vo.setWarnings(warnings);
        vo.setAddress(addressService.getOwnedOrDefault(userId, parseId(request.getAddressId())));
        vo.setCoupons(couponService.brief(userId, goodsCent, couponId));
        return vo;
    }

    private AmountVO toAmount(PriceCalculator.PriceResult result) {
        AmountVO vo = new AmountVO();
        vo.setGoodsAmount(PriceUtil.toYuan(result.goodsCent()));
        vo.setDiscount(PriceUtil.toYuan(result.discountCent()));
        vo.setShipFee(PriceUtil.toYuan(result.shipFeeCent()));
        vo.setPayable(PriceUtil.toYuan(result.payableCent()));
        vo.setSaved(PriceUtil.toYuan(result.savedCent()));
        return vo;
    }

    private ShippingVO toShipping(PriceCalculator.PriceResult result) {
        long threshold = priceCalculator.freeThresholdCent();
        ShippingVO vo = new ShippingVO();
        vo.setFreeShippingAt(PriceUtil.toYuan(threshold));
        vo.setToFreeShipping(PriceUtil.toYuan(Math.max(0L, threshold - (result.goodsCent() - result.discountCent()))));
        vo.setIsFreeShipping(result.shipFeeCent() == 0);
        return vo;
    }

    static Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "id 格式不正确");
        }
    }
}
