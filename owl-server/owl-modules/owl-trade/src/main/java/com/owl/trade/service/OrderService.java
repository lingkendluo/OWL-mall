package com.owl.trade.service;

import com.owl.common.api.PageResult;
import com.owl.trade.dto.OrderCancelRequest;
import com.owl.trade.dto.OrderCreateRequest;
import com.owl.trade.vo.OrderBriefVO;
import com.owl.trade.vo.OrderCountVO;
import com.owl.trade.vo.OrderCreateVO;
import com.owl.trade.vo.OrderDetailVO;

public interface OrderService {

    OrderCreateVO create(Long userId, OrderCreateRequest request);

    PageResult<OrderBriefVO> list(Long userId, String status, int page, int size);

    OrderDetailVO detail(Long userId, String orderNo);

    OrderDetailVO cancel(Long userId, String orderNo, OrderCancelRequest request);

    OrderDetailVO confirm(Long userId, String orderNo);

    OrderCountVO counts(Long userId);

    // ---------- B 端 ----------

    PageResult<OrderBriefVO> adminList(String status, String keyword, int page, int size);

    OrderDetailVO adminDetail(String orderNo);

    OrderDetailVO deliver(String orderNo, String company, String trackingNo);

    /** 定时任务：关闭超时未支付订单，返回处理条数 */
    int closeExpiredOrders(int expireMinutes);
}
