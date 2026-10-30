package com.owl.payment.port;

import java.time.LocalDateTime;

/**
 * 支付模块需要订单信息，但订单归 trade 管。
 * 用这个端口把依赖倒过来：payment 定义接口，trade 实现，两边不互相 import 实体。
 */
public interface PayOrderPort {

    PayOrderInfo load(String orderNo);

    /** 支付成功：订单 10 → 20，pay_status=1，写流水、发消息 */
    void markPaid(String orderNo, String payNo);

    /** 退款成功：订单 → 70，pay_status=2，回补库存、发消息 */
    void markRefunded(String orderNo, String payNo, String refundNo);

    record PayOrderInfo(
            String orderNo,
            Long userId,
            int orderStatus,
            int payStatus,
            long payAmountCent,
            LocalDateTime expireAt,
            String firstItemName
    ) {
    }
}
