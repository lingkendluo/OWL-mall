package com.owl.payment.controller;

import com.owl.common.api.Result;
import com.owl.common.context.UserContext;
import com.owl.payment.dto.MockCallbackRequest;
import com.owl.payment.dto.RefundRequest;
import com.owl.payment.service.PaymentService;
import com.owl.payment.service.RefundService;
import com.owl.payment.vo.PayCallbackVO;
import com.owl.payment.vo.PayDetailVO;
import com.owl.payment.vo.PayVO;
import com.owl.payment.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @Operation(summary = "发起支付（模拟渠道）")
    @PostMapping("/mock/{orderNo}")
    public Result<PayVO> mockPay(@PathVariable String orderNo) {
        return Result.ok(paymentService.initiate(orderNo, UserContext.requireUserId()));
    }

    @Operation(summary = "模拟支付回调")
    @PostMapping("/mock/callback")
    public Result<PayCallbackVO> callback(@Valid @RequestBody MockCallbackRequest request) {
        return Result.ok(paymentService.callback(request));
    }

    @Operation(summary = "查询支付单")
    @GetMapping("/{payNo}")
    public Result<PayDetailVO> detail(@PathVariable String payNo) {
        return Result.ok(paymentService.detail(payNo, UserContext.requireUserId()));
    }

    @Operation(summary = "申请退款")
    @PostMapping("/refund")
    public Result<RefundVO> refund(@Valid @RequestBody RefundRequest request) {
        return Result.ok(refundService.apply(UserContext.requireUserId(), request));
    }

    @Operation(summary = "查询退款单")
    @GetMapping("/refunds/{refundNo}")
    public Result<RefundVO> refundDetail(@PathVariable String refundNo) {
        return Result.ok(refundService.detail(refundNo, UserContext.requireUserId()));
    }
}
