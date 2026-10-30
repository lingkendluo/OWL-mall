package com.owl.payment.service;

import com.owl.payment.dto.MockCallbackRequest;
import com.owl.payment.vo.PayCallbackVO;
import com.owl.payment.vo.PayDetailVO;
import com.owl.payment.vo.PayVO;

import java.time.LocalDateTime;

public interface PaymentService {

    /** 下单时顺手建一张待支付单，返回 payNo（OrderCreateVO 里要用） */
    String createForOrder(String orderNo, Long userId, long amountCent, LocalDateTime expireAt);

    /** 发起支付：已有待支付单就复用，不重复创建 */
    PayVO initiate(String orderNo, Long userId);

    /** 模拟渠道回调，幂等 */
    PayCallbackVO callback(MockCallbackRequest request);

    PayDetailVO detail(String payNo, Long userId);

    /** 订单详情要带 payNo，订单表不存这个字段，从支付单反查 */
    String findPayNoByOrder(String orderNo);
}
