package com.owl.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额换算。库里一律分（long），对外一律元（BigDecimal，两位小数）。
 */
public final class PriceUtil {

    private PriceUtil() {
    }

    public static BigDecimal toYuan(long cent) {
        return BigDecimal.valueOf(cent, 2);   // 9900 -> 99.00
    }

    public static long toCent(BigDecimal yuan) {
        if (yuan == null) {
            return 0L;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** 元 → 分，允许 null，返回 0 */
    public static long toCentOrZero(BigDecimal yuan) {
        return yuan == null ? 0L : toCent(yuan);
    }

    /** 负数归零，用于运费差这类展示字段 */
    public static long maxZero(long value) {
        return Math.max(0L, value);
    }
}
