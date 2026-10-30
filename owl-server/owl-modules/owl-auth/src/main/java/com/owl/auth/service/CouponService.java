package com.owl.auth.service;

import com.owl.auth.vo.CouponBriefVO;
import com.owl.auth.vo.CouponVO;
import com.owl.common.api.PageResult;

import java.util.List;

public interface CouponService {

    PageResult<CouponVO> list(Long userId, String status, int page, int size);

    /** 结算页的可用券列表：不可用的也返回，带 reason，不报错 */
    List<CouponBriefVO> brief(Long userId, long goodsCent, Long selectedUserCouponId);

    /** 券是否可用 + 能减多少 */
    CouponCheck check(Long userCouponId, Long userId, long goodsCent);

    /** 下单锁券，抢不到抛 40005 */
    void lock(Long userCouponId, Long userId, String orderNo);

    /** 取消 / 超时 / 退款时释放 */
    void releaseByOrder(String orderNo);

    void grant(Long userId, String couponCode);

    void grantNewUser(Long userId);

    /** discountCent 为实际减免；usable=false 时 reason 说明原因 */
    record CouponCheck(long discountCent, boolean usable, String reason) {
        public static CouponCheck none(String reason) {
            return new CouponCheck(0L, false, reason);
        }
    }
}
