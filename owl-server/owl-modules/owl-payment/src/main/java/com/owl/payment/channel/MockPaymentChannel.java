package com.owl.payment.channel;

import com.owl.common.util.IdUtils;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 模拟渠道。行为按真实渠道写：有凭证、有有效期、回调要校验、可重复回调。
 */
@Component
@RequiredArgsConstructor
public class MockPaymentChannel implements PaymentChannel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final KVStore kv;

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ChannelPay create(String payNo, long amountCent, LocalDateTime expireAt) {
        String token = "mock_" + IdUtils.uuid().substring(0, 16);
        Duration ttl = Duration.between(LocalDateTime.now(), expireAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(30);
        }
        kv.set(RedisKey.payMock(payNo), token, ttl);
        return new ChannelPay(payNo, token, expireAt);
    }

    @Override
    public boolean verifyCallback(String payNo, String token) {
        if (payNo == null || token == null) {
            return false;
        }
        String expected = kv.get(RedisKey.payMock(payNo));
        return expected != null && expected.equals(token);
    }

    @Override
    public String outTradeNo(String payNo) {
        return "MOCK" + LocalDateTime.now().format(TIME) + payNo.substring(Math.max(0, payNo.length() - 6));
    }

    @Override
    public ChannelRefund refund(String payNo, String refundNo, long amountCent, String reason) {
        // 模拟渠道同步成功；真实渠道这里会拿渠道返回码
        return new ChannelRefund(refundNo, true, "MOCKR" + LocalDateTime.now().format(TIME) + refundNo.substring(Math.max(0, refundNo.length() - 4)));
    }
}
