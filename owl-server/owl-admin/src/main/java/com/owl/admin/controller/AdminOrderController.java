package com.owl.admin.controller;

import com.owl.admin.aop.AdminAction;
import com.owl.admin.dto.DeliverRequest;
import com.owl.common.api.PageResult;
import com.owl.common.api.Result;
import com.owl.trade.service.OrderService;
import com.owl.trade.vo.OrderBriefVO;
import com.owl.trade.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-订单")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单列表")
    @GetMapping
    public Result<PageResult<OrderBriefVO>> list(@RequestParam(defaultValue = "all") String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.adminList(status, keyword,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.adminDetail(orderNo));
    }

    @Operation(summary = "发货")
    @AdminAction("ORDER_DELIVER")
    @PostMapping("/{orderNo}/deliver")
    public Result<OrderDetailVO> deliver(@PathVariable String orderNo,
                                         @RequestBody(required = false) DeliverRequest request) {
        String company = request == null ? null : request.getCompany();
        String trackingNo = request == null ? null : request.getTrackingNo();
        return Result.ok(orderService.deliver(orderNo, company, trackingNo));
    }
}
