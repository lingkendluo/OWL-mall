package com.owl.trade.service;

import com.owl.infra.config.ShippingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 计价的四个边界，全是分，不出现浮点。
 */
class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator(new ShippingProperties());

    private PriceCalculator.PriceLine line(long priceCent, long listCent, int qty) {
        return new PriceCalculator.PriceLine("snacks-1", priceCent, listCent, qty);
    }

    @Test
    @DisplayName("满 99 元免运费")
    void freeShippingAtThreshold() {
        PriceCalculator.PriceResult result = calculator.calculate(List.of(line(9900L, 12900L, 1)), 0L);
        assertEquals(9900L, result.goodsCent());
        assertEquals(0L, result.shipFeeCent());
        assertEquals(9900L, result.payableCent());
        assertEquals(3000L, result.savedCent());
    }

    @Test
    @DisplayName("不满 99 元收 5 元运费")
    void chargeShippingBelowThreshold() {
        PriceCalculator.PriceResult result = calculator.calculate(List.of(line(5000L, 5000L, 1)), 0L);
        assertEquals(5000L, result.goodsCent());
        assertEquals(500L, result.shipFeeCent());
        assertEquals(5500L, result.payableCent());
    }

    @Test
    @DisplayName("用券后不满 99 元，仍然收运费")
    void shippingJudgedAfterDiscount() {
        // 100 元减 20 元 = 80 元，落在门槛下，要收 5 元
        PriceCalculator.PriceResult result = calculator.calculate(List.of(line(10000L, 10000L, 1)), 2000L);
        assertEquals(10000L, result.goodsCent());
        assertEquals(2000L, result.discountCent());
        assertEquals(500L, result.shipFeeCent());
        assertEquals(8500L, result.payableCent());
    }

    @Test
    @DisplayName("券面额超过商品金额时，最多减到 0，运费照收")
    void discountCannotExceedGoods() {
        PriceCalculator.PriceResult result = calculator.calculate(List.of(line(500L, 500L, 1)), 2000L);
        assertEquals(500L, result.discountCent());
        assertEquals(0L, result.payableCent() - result.shipFeeCent());
        assertEquals(500L, result.payableCent());
    }
}
