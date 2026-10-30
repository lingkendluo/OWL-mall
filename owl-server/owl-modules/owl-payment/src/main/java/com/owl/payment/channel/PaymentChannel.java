package com.owl.payment.channel;

import java.time.LocalDateTime;

/**
 * 支付渠道 SPI。加真实渠道时实现这个接口，Service 里不许出现 if (channel.equals("mock"))。
 *
 * 没有 query()：模拟渠道的支付状态以 p_payment 行为准，异步查单是真实渠道才需要的动作，
 * 等接微信/支付宝时再加，不提前留空方法。
 */
public interface PaymentChannel {

    String name();

    /** 发起支付，返回渠道凭证（模拟渠道是 mockToken，真实渠道通常是预支付 id） */
    ChannelPay create(String payNo, long amountCent, LocalDateTime expireAt);

    /** 校验回调合法性（模拟渠道比对 mockToken，真实渠道验签） */
    boolean verifyCallback(String payNo, String token);

    /** 渠道流水号，落 p_payment.out_trade_no */
    String outTradeNo(String payNo);

    ChannelRefund refund(String payNo, String refundNo, long amountCent, String reason);

    record ChannelPay(String payNo, String token, LocalDateTime expireAt) {
    }

    record ChannelRefund(String refundNo, boolean success, String channelRefundNo) {
    }
}
