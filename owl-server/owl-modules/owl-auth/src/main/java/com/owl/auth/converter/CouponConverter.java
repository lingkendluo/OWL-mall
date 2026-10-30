package com.owl.auth.converter;

import com.owl.auth.entity.MkCoupon;
import com.owl.auth.entity.MkUserCoupon;
import com.owl.auth.vo.CouponVO;
import com.owl.common.util.PriceUtil;

import java.time.ZoneId;

public final class CouponConverter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private CouponConverter() {
    }

    public static CouponVO toVO(MkUserCoupon userCoupon, MkCoupon coupon, boolean usable) {
        CouponVO vo = new CouponVO();
        vo.setId(String.valueOf(userCoupon.getId()));
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setThreshold(PriceUtil.toYuan(coupon.getThresholdCent() == null ? 0L : coupon.getThresholdCent()));
        vo.setDiscount(PriceUtil.toYuan(coupon.getDiscountCent() == null ? 0L : coupon.getDiscountCent()));
        vo.setStartAt(coupon.getStartAt() == null ? null : coupon.getStartAt().atZone(ZONE).toInstant().toEpochMilli());
        vo.setEndAt(coupon.getEndAt() == null ? null : coupon.getEndAt().atZone(ZONE).toInstant().toEpochMilli());
        vo.setStatus(userCoupon.getStatus());
        vo.setUsable(usable);
        return vo;
    }

    public static long toMillis(java.time.LocalDateTime time) {
        return time == null ? 0L : time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
