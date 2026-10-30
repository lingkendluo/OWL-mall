package com.owl.trade.service;

import com.owl.infra.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 计价唯一入口。内部全程 long 分，只在 VO 层转元。
 * 规则：运费按「优惠后金额」判定，不是商品原价。
 */
@Component
@RequiredArgsConstructor
public class PriceCalculator {

    private final ShippingProperties shipping;

    public record PriceLine(String productCode, long priceCent, long listCent, int qty) {
    }

    public record PriceResult(long goodsCent, long discountCent, long shipFeeCent, long payableCent, long savedCent) {
    }

    public PriceResult calculate(List<PriceLine> lines, long discountCent) {
        long goods = 0L;
        long saved = 0L;
        for (PriceLine line : lines) {
            goods += line.priceCent() * line.qty();
            saved += Math.max(0L, line.listCent() - line.priceCent()) * line.qty();
        }
        long discount = Math.min(Math.max(0L, discountCent), goods);
        long afterDiscount = Math.max(0L, goods - discount);
        long shipFee = afterDiscount >= shipping.getFreeThresholdCent() ? 0L : shipping.getFeeCent();
        long payable = afterDiscount + shipFee;
        return new PriceResult(goods, discount, shipFee, payable, saved);
    }

    /** 只算商品部分，购物车 summary 用 */
    public PriceResult calculateGoods(List<PriceLine> lines) {
        return calculate(lines, 0L);
    }

    public long freeThresholdCent() {
        return shipping.getFreeThresholdCent();
    }

    public long shipFeeCent() {
        return shipping.getFeeCent();
    }
}
