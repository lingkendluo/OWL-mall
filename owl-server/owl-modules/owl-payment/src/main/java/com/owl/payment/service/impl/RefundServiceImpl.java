package com.owl.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.IdUtils;
import com.owl.common.util.PriceUtil;
import com.owl.payment.channel.PaymentChannel;
import com.owl.payment.dto.RefundRequest;
import com.owl.payment.entity.PPayment;
import com.owl.payment.entity.PRefund;
import com.owl.payment.mapper.PPaymentMapper;
import com.owl.payment.mapper.PRefundMapper;
import com.owl.payment.port.PayOrderPort;
import com.owl.payment.service.RefundService;
import com.owl.payment.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PRefundMapper refundMapper;
    private final PPaymentMapper paymentMapper;
    private final PayOrderPort orderPort;
    private final PaymentChannel channel;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(Long userId, RefundRequest request) {
        PayOrderPort.PayOrderInfo order = orderPort.load(request.getOrderNo());
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.userId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        // 先判重：同一订单已有处理中/已成功的退款单。退款成功后 pay_status 变成 2，
        // 若先判支付状态会返回"订单未支付"，跟文档要求的 50003 不一致。
        Long exists = refundMapper.selectCount(new LambdaQueryWrapper<PRefund>()
                .eq(PRefund::getOrderNo, request.getOrderNo())
                .in(PRefund::getStatus, 0, 1));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.REFUND_DUPLICATE);
        }
        if (order.payStatus() != 1) {
            throw new BizException(ErrorCode.PAY_STATUS_INVALID, "订单未支付，请走取消");
        }
        int status = order.orderStatus();
        if (status != 20 && status != 30 && status != 40) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "当前订单状态不支持退款");
        }

        PPayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                .eq(PPayment::getOrderNo, request.getOrderNo())
                .eq(PPayment::getStatus, 1)
                .orderByDesc(PPayment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            throw new BizException(ErrorCode.PAY_NOT_FOUND);
        }

        PRefund refund = new PRefund();
        refund.setRefundNo(nextRefundNo());
        refund.setPayNo(payment.getPayNo());
        refund.setOrderNo(request.getOrderNo());
        refund.setUserId(userId);
        // 本期全额退，不支持部分退
        refund.setAmountCent(payment.getAmountCent());
        refund.setReason(request.getReason());
        refund.setStatus(0);
        refundMapper.insert(refund);

        PaymentChannel.ChannelRefund result = channel.refund(payment.getPayNo(), refund.getRefundNo(),
                refund.getAmountCent(), request.getReason());
        if (!result.success()) {
            refundMapper.update(null, new LambdaUpdateWrapper<PRefund>()
                    .eq(PRefund::getId, refund.getId())
                    .set(PRefund::getStatus, 2));
            throw new BizException(ErrorCode.PAY_STATUS_INVALID, "渠道退款失败，请联系客服");
        }

        // 模拟渠道同步成功：退款单 1、支付单 4、订单 70
        refundMapper.update(null, new LambdaUpdateWrapper<PRefund>()
                .eq(PRefund::getId, refund.getId())
                .set(PRefund::getStatus, 1)
                .set(PRefund::getChannelRefundNo, result.channelRefundNo()));
        paymentMapper.update(null, new LambdaUpdateWrapper<PPayment>()
                .eq(PPayment::getId, payment.getId())
                .set(PPayment::getStatus, 4));
        orderPort.markRefunded(order.orderNo(), payment.getPayNo(), refund.getRefundNo());

        RefundVO vo = new RefundVO();
        vo.setRefundNo(refund.getRefundNo());
        vo.setAmount(PriceUtil.toYuan(refund.getAmountCent()));
        vo.setStatus(1);
        return vo;
    }

    @Override
    public RefundVO detail(String refundNo, Long userId) {
        PRefund refund = refundMapper.selectOne(new LambdaQueryWrapper<PRefund>()
                .eq(PRefund::getRefundNo, refundNo)
                .eq(PRefund::getUserId, userId));
        if (refund == null) {
            throw new BizException(ErrorCode.PAY_NOT_FOUND, "退款单不存在");
        }
        RefundVO vo = new RefundVO();
        vo.setRefundNo(refund.getRefundNo());
        vo.setAmount(PriceUtil.toYuan(refund.getAmountCent()));
        vo.setStatus(refund.getStatus());
        return vo;
    }

    private String nextRefundNo() {
        return "R" + LocalDateTime.now().format(TIME) + IdUtils.randomDigits(5);
    }
}
