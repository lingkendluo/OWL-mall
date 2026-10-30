package com.owl.trade.controller;

import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import com.owl.trade.dto.OrderCancelRequest;
import com.owl.trade.dto.OrderCreateRequest;
import com.owl.trade.dto.PreviewRequest;
import com.owl.trade.service.CheckoutService;
import com.owl.trade.service.OrderService;
import com.owl.trade.vo.OrderBriefVO;
import com.owl.trade.vo.OrderCountVO;
import com.owl.trade.vo.OrderCreateVO;
import com.owl.trade.vo.OrderDetailVO;
import com.owl.trade.vo.PreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算与订单。价格全部服务端重算，前端传什么价都不认。
 */
@Tag(name = "结算下单")
@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
public class TradeController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @Operation(summary = "结算预览")
    @PostMapping("/preview")
    public Result<PreviewVO> preview(@Valid @RequestBody PreviewRequest request) {
        return Result.ok(checkoutService.preview(UserContext.requireUserId(), request));
    }

    @Operation(summary = "创建订单")
    @PostMapping("/orders")
    public Result<OrderCreateVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok(orderService.create(UserContext.requireUserId(), request));
    }

    @Operation(summary = "订单角标")
    @GetMapping("/orders/counts")
    public Result<OrderCountVO> counts() {
        return Result.ok(orderService.counts(UserContext.requireUserId()));
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderBriefVO>> list(@RequestParam(defaultValue = "all") String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.list(UserContext.requireUserId(), status,
                Math.max(1, page), Math.min(50, Math.max(1, size))));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(UserContext.requireUserId(), orderNo));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{orderNo}/cancel")
    public Result<OrderDetailVO> cancel(@PathVariable String orderNo,
                                        @RequestBody(required = false) OrderCancelRequest request) {
        return Result.ok(orderService.cancel(UserContext.requireUserId(), orderNo, request));
    }

    @Operation(summary = "确认收货")
    @PostMapping("/orders/{orderNo}/confirm")
    public Result<OrderDetailVO> confirm(@PathVariable String orderNo) {
        return Result.ok(orderService.confirm(UserContext.requireUserId(), orderNo));
    }
}
