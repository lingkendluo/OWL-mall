package com.owl.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.IdUtils;
import com.owl.common.util.PriceUtil;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import com.owl.payment.channel.PaymentChannel;
import com.owl.payment.dto.MockCallbackRequest;
import com.owl.payment.entity.PPayment;
import com.owl.payment.mapper.PPaymentMapper;
import com.owl.payment.port.PayOrderPort;
import com.owl.payment.service.PaymentService;
import com.owl.payment.vo.PayCallbackVO;
import com.owl.payment.vo.PayDetailVO;
import com.owl.payment.vo.PayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter PAY_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final PPaymentMapper paymentMapper;
    private final ObjectProvider<PayOrderPort> orderPortProvider;
    private final PaymentChannel channel;
    private final KVStore kv;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createForOrder(String orderNo, Long userId, long amountCent, LocalDateTime expireAt) {
        PPayment exists = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                .eq(PPayment::getOrderNo, orderNo)
                .eq(PPayment::getStatus, 0)
                .orderByDesc(PPayment::getId)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists.getPayNo();
        }
        PPayment payment = new PPayment();
        payment.setPayNo(nextPayNo());
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setChannel(channel.name());
        payment.setAmountCent(amountCent);
        payment.setStatus(0);
        payment.setExpireAt(expireAt);
        paymentMapper.insert(payment);
        return payment.getPayNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayVO initiate(String orderNo, Long userId) {
        PayOrderPort.PayOrderInfo order = orderPort().load(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.userId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.orderStatus() == 50) {
            throw new BizException(ErrorCode.PAY_STATUS_INVALID, "订单已取消，不能支付");
        }
        if (order.orderStatus() != 10) {
            throw new BizException(ErrorCode.PAY_STATUS_INVALID, "订单已支付或已关闭");
        }
        if (order.expireAt() != null && order.expireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_EXPIRED);
        }

        PPayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                .eq(PPayment::getOrderNo, orderNo)
                .eq(PPayment::getStatus, 0)
                .orderByDesc(PPayment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            String payNo = createForOrder(orderNo, userId, order.payAmountCent(), order.expireAt());
            payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>().eq(PPayment::getPayNo, payNo));
        }

        PaymentChannel.ChannelPay created = channel.create(payment.getPayNo(), payment.getAmountCent(), order.expireAt());
        PayVO vo = new PayVO();
        vo.setPayNo(payment.getPayNo());
        vo.setChannel(payment.getChannel());
        vo.setAmount(PriceUtil.toYuan(payment.getAmountCent()));
        vo.setMockToken(created.token());
        vo.setExpireAt(order.expireAt() == null ? null : order.expireAt().atZone(ZONE).toInstant().toEpochMilli());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayCallbackVO callback(MockCallbackRequest request) {
        String lockKey = RedisKey.payLock(request.getPayNo());
        boolean locked = kv.setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!locked) {
            throw new BizException(ErrorCode.REPEAT_SUBMIT, "回调处理中，请稍后");
        }
        try {
            PPayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                    .eq(PPayment::getPayNo, request.getPayNo()));
            if (payment == null) {
                throw new BizException(ErrorCode.PAY_NOT_FOUND);
            }
            // 已成功过一次的重复回调直接放行：不再验凭证（首次成功时凭证已作废），也不动订单和钱
            if (payment.getStatus() != null && payment.getStatus() == 1) {
                return buildCallback(payment.getOrderNo(), 1);
            }
            if (!channel.verifyCallback(request.getPayNo(), request.getMockToken())) {
                throw new BizException(ErrorCode.PAY_NOT_FOUND, "回调凭证无效");
            }

            if (!Boolean.TRUE.equals(request.getSuccess())) {
                paymentMapper.update(null, new LambdaUpdateWrapper<PPayment>()
                        .eq(PPayment::getId, payment.getId())
                        .eq(PPayment::getStatus, 0)
                        .set(PPayment::getStatus, 2));
                return buildCallback(payment.getOrderNo(), 0);
            }

            // UPDATE ... WHERE status=0，影响 0 行说明已被处理
            int rows = paymentMapper.update(null, new LambdaUpdateWrapper<PPayment>()
                    .eq(PPayment::getPayNo, request.getPayNo())
                    .eq(PPayment::getStatus, 0)
                    .set(PPayment::getStatus, 1)
                    .set(PPayment::getPaidAt, LocalDateTime.now())
                    .set(PPayment::getOutTradeNo, channel.outTradeNo(request.getPayNo())));
            if (rows == 0) {
                return buildCallback(payment.getOrderNo(), 1);
            }

            orderPort().markPaid(payment.getOrderNo(), payment.getPayNo());
            kv.delete(RedisKey.payMock(payment.getPayNo()));
            return buildCallback(payment.getOrderNo(), 1);
        } finally {
            kv.delete(lockKey);
        }
    }

    @Override
    public PayDetailVO detail(String payNo, Long userId) {
        PPayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                .eq(PPayment::getPayNo, payNo)
                .eq(PPayment::getUserId, userId));
        if (payment == null) {
            throw new BizException(ErrorCode.PAY_NOT_FOUND);
        }
        PayDetailVO vo = new PayDetailVO();
        vo.setPayNo(payment.getPayNo());
        vo.setOrderNo(payment.getOrderNo());
        vo.setChannel(payment.getChannel());
        vo.setAmount(PriceUtil.toYuan(payment.getAmountCent()));
        vo.setStatus(payment.getStatus());
        vo.setPaidAt(payment.getPaidAt() == null ? null : payment.getPaidAt().atZone(ZONE).toInstant().toEpochMilli());
        return vo;
    }

    @Override
    public String findPayNoByOrder(String orderNo) {
        PPayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<PPayment>()
                .eq(PPayment::getOrderNo, orderNo)
                .orderByDesc(PPayment::getId)
                .last("LIMIT 1"));
        return payment == null ? null : payment.getPayNo();
    }

    private PayCallbackVO buildCallback(String orderNo, int payStatus) {
        PayCallbackVO vo = new PayCallbackVO();
        vo.setOrderNo(orderNo);
        vo.setPayStatus(payStatus);
        PayOrderPort.PayOrderInfo order = orderPort().load(orderNo);
        vo.setOrderStatus(order == null ? null : order.orderStatus());
        return vo;
    }

    /**
     * 订单侧也要注入支付服务（下单即建支付单），直接把 PayOrderPort 拿到构造器里会形成循环依赖。
     * 用 ObjectProvider 延迟到真正调用时再取，此时容器已就绪。
     */
    private PayOrderPort orderPort() {
        return orderPortProvider.getObject();
    }

    private String nextPayNo() {
        return "P" + LocalDateTime.now().format(PAY_NO_TIME) + IdUtils.randomDigits(6);
    }
}
