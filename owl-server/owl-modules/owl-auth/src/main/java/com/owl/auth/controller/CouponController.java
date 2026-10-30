package com.owl.auth.controller;

import com.owl.auth.service.CouponService;
import com.owl.auth.service.PointsService;
import com.owl.auth.vo.CouponVO;
import com.owl.auth.vo.PointsLogVO;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人中心-券与积分")
@RestController
@RequestMapping("/m")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final PointsService pointsService;

    @Operation(summary = "我的优惠券")
    @GetMapping("/coupons")
    public Result<PageResult<CouponVO>> coupons(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(couponService.list(UserContext.requireUserId(), status, Math.max(1, page), Math.min(100, size)));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/points")
    public Result<PageResult<PointsLogVO>> points(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return Result.ok(pointsService.list(UserContext.requireUserId(), Math.max(1, page), Math.min(100, size)));
    }
}
