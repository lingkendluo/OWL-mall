package com.owl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.auth.converter.CouponConverter;
import com.owl.auth.entity.MkCoupon;
import com.owl.auth.entity.MkUserCoupon;
import com.owl.auth.mapper.MkCouponMapper;
import com.owl.auth.mapper.MkUserCouponMapper;
import com.owl.auth.service.CouponService;
import com.owl.auth.vo.CouponBriefVO;
import com.owl.auth.vo.CouponVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.PriceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final MkUserCouponMapper userCouponMapper;
    private final MkCouponMapper couponMapper;

    @Override
    public PageResult<CouponVO> list(Long userId, String status, int page, int size) {
        expireLazily(userId);

        LambdaQueryWrapper<MkUserCoupon> wrapper = new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getUserId, userId)
                .orderByDesc(MkUserCoupon::getReceivedAt);
        if (status != null && !status.isBlank()) {
            switch (status) {
                case "unused" -> wrapper.eq(MkUserCoupon::getStatus, 0);
                case "used" -> wrapper.eq(MkUserCoupon::getStatus, 1);
                case "expired" -> wrapper.eq(MkUserCoupon::getStatus, 2);
                default -> {
                }
            }
        }
        Page<MkUserCoupon> result = userCouponMapper.selectPage(Page.of(page, size), wrapper);
        Map<Long, MkCoupon> templates = loadTemplates(result.getRecords());
        LocalDateTime now = LocalDateTime.now();
        List<CouponVO> list = result.getRecords().stream()
                .map(uc -> {
                    MkCoupon template = templates.get(uc.getCouponId());
                    boolean usable = uc.getStatus() == 0 && template != null
                            && !now.isBefore(template.getStartAt()) && !now.isAfter(template.getEndAt());
                    return CouponConverter.toVO(uc, template == null ? new MkCoupon() : template, usable);
                })
                .toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public List<CouponBriefVO> brief(Long userId, long goodsCent, Long selectedUserCouponId) {
        expireLazily(userId);
        List<MkUserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getUserId, userId)
                .eq(MkUserCoupon::getStatus, 0));
        if (coupons.isEmpty()) {
            return List.of();
        }
        Map<Long, MkCoupon> templates = loadTemplates(coupons);
        List<CouponBriefVO> result = new ArrayList<>();
        for (MkUserCoupon uc : coupons) {
            CouponCheck check = check(uc.getId(), userId, goodsCent);
            CouponBriefVO vo = new CouponBriefVO();
            vo.setId(String.valueOf(uc.getId()));
            MkCoupon template = templates.get(uc.getCouponId());
            vo.setName(template == null ? "" : template.getName());
            vo.setDiscount(PriceUtil.toYuan(check.discountCent()));
            vo.setUsable(check.usable());
            vo.setReason(check.reason());
            result.add(vo);
        }
        // 能给用户省最多的排前面
        result.sort(Comparator.comparing(CouponBriefVO::getDiscount).reversed());
        return result;
    }

    @Override
    public CouponCheck check(Long userCouponId, Long userId, long goodsCent) {
        if (userCouponId == null) {
            return CouponCheck.none("未选择优惠券");
        }
        MkUserCoupon userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getId, userCouponId)
                .eq(MkUserCoupon::getUserId, userId));
        if (userCoupon == null) {
            return CouponCheck.none("优惠券不存在");
        }
        if (userCoupon.getStatus() != 0) {
            return CouponCheck.none("优惠券已使用或已过期");
        }
        MkCoupon template = couponMapper.selectById(userCoupon.getCouponId());
        if (template == null || template.getStatus() != 1) {
            return CouponCheck.none("优惠券已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getStartAt())) {
            return CouponCheck.none("优惠券尚未生效");
        }
        if (now.isAfter(template.getEndAt())) {
            return CouponCheck.none("优惠券已过期");
        }
        long threshold = template.getThresholdCent() == null ? 0L : template.getThresholdCent();
        if (goodsCent < threshold) {
            return CouponCheck.none("还差 ¥" + PriceUtil.toYuan(threshold - goodsCent).toPlainString() + " 可用");
        }
        long discount;
        if (template.getType() != null && template.getType() == 2 && template.getDiscountRate() != null) {
            discount = goodsCent * (100 - template.getDiscountRate()) / 100;
        } else {
            long face = template.getDiscountCent() == null ? 0L : template.getDiscountCent();
            discount = Math.min(face, goodsCent);
        }
        if (discount <= 0) {
            return CouponCheck.none("优惠券不可用");
        }
        return new CouponCheck(discount, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long userCouponId, Long userId, String orderNo) {
        if (userCouponId == null) {
            return;
        }
        int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getId, userCouponId)
                .eq(MkUserCoupon::getUserId, userId)
                .eq(MkUserCoupon::getStatus, 0)
                .set(MkUserCoupon::getStatus, 1)
                .set(MkUserCoupon::getOrderNo, orderNo)
                .set(MkUserCoupon::getUsedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.COUPON_UNUSABLE, "优惠券已被使用，请重新选择");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseByOrder(String orderNo) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getOrderNo, orderNo)
                .eq(MkUserCoupon::getStatus, 1)
                .set(MkUserCoupon::getStatus, 0)
                .set(MkUserCoupon::getOrderNo, null)
                .set(MkUserCoupon::getUsedAt, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long userId, String couponCode) {
        MkCoupon template = couponMapper.selectOne(new LambdaQueryWrapper<MkCoupon>()
                .eq(MkCoupon::getCode, couponCode)
                .eq(MkCoupon::getStatus, 1));
        if (template == null) {
            log.warn("券模板不存在或已停用: {}", couponCode);
            return;
        }
        MkUserCoupon userCoupon = new MkUserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(template.getId());
        userCoupon.setStatus(0);
        userCoupon.setReceivedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);

        couponMapper.update(null, new LambdaUpdateWrapper<MkCoupon>()
                .eq(MkCoupon::getId, template.getId())
                .setSql("received = received + 1"));
    }

    @Override
    public void grantNewUser(Long userId) {
        grant(userId, "NEW10");
    }

    /** 惰性过期：查询时顺手把过期未用的置 2，不靠定时任务兜底 */
    private void expireLazily(Long userId) {
        List<MkUserCoupon> unused = userCouponMapper.selectList(new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getUserId, userId)
                .eq(MkUserCoupon::getStatus, 0));
        if (unused.isEmpty()) {
            return;
        }
        Map<Long, MkCoupon> templates = loadTemplates(unused);
        List<Long> expiredIds = unused.stream()
                .filter(uc -> {
                    MkCoupon template = templates.get(uc.getCouponId());
                    return template != null && template.getEndAt() != null
                            && template.getEndAt().isBefore(LocalDateTime.now());
                })
                .map(MkUserCoupon::getId)
                .toList();
        if (!expiredIds.isEmpty()) {
            userCouponMapper.update(null, new LambdaUpdateWrapper<MkUserCoupon>()
                    .in(MkUserCoupon::getId, expiredIds)
                    .set(MkUserCoupon::getStatus, 2));
        }
    }

    private Map<Long, MkCoupon> loadTemplates(List<MkUserCoupon> coupons) {
        if (coupons.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = coupons.stream().map(MkUserCoupon::getCouponId).distinct().toList();
        return couponMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MkCoupon::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }
}
