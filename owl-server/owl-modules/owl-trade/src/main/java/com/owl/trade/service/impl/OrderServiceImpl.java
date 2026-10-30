package com.owl.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.auth.service.AddressService;
import com.owl.auth.service.CouponService;
import com.owl.auth.vo.AddressVO;
import com.owl.catalog.api.ProductSnapshot;
import com.owl.catalog.service.ProductService;
import com.owl.catalog.service.StockService;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.IdUtils;
import com.owl.common.util.PriceUtil;
import com.owl.infra.config.TradeProperties;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import com.owl.message.enums.MessageKind;
import com.owl.message.enums.MessageTemplate;
import com.owl.message.service.MessageService;
import com.owl.payment.port.PayOrderPort;
import com.owl.payment.service.PaymentService;
import com.owl.trade.dto.OrderCancelRequest;
import com.owl.trade.dto.OrderCreateRequest;
import com.owl.trade.dto.OrderLineDTO;
import com.owl.trade.entity.TOrder;
import com.owl.trade.entity.TOrderItem;
import com.owl.trade.entity.TOrderStatusLog;
import com.owl.trade.enums.OrderStatus;
import com.owl.trade.mapper.TOrderItemMapper;
import com.owl.trade.mapper.TOrderMapper;
import com.owl.trade.mapper.TOrderStatusLogMapper;
import com.owl.trade.service.CartService;
import com.owl.trade.service.OrderService;
import com.owl.trade.service.PriceCalculator;
import com.owl.trade.vo.AmountVO;
import com.owl.trade.vo.OrderBriefVO;
import com.owl.trade.vo.OrderCountVO;
import com.owl.trade.vo.OrderCreateVO;
import com.owl.trade.vo.OrderDetailVO;
import com.owl.trade.vo.OrderItemVO;
import com.owl.trade.vo.TimelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService, PayOrderPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TOrderMapper orderMapper;
    private final TOrderItemMapper orderItemMapper;
    private final TOrderStatusLogMapper statusLogMapper;
    private final CartService cartService;
    private final ProductService productService;
    private final StockService stockService;
    private final CouponService couponService;
    private final AddressService addressService;
    private final PaymentService paymentService;
    private final MessageService messageService;
    private final PriceCalculator priceCalculator;
    private final TradeProperties tradeProperties;
    private final KVStore kv;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(Long userId, OrderCreateRequest request) {
        String tokenKey = (request.getClientToken() == null || request.getClientToken().isBlank())
                ? null : RedisKey.tradeToken(userId, request.getClientToken());

        if (tokenKey != null) {
            String exist = kv.get(tokenKey);
            if (exist != null && !"PENDING".equals(exist)) {
                // 同一 clientToken 重复提交：直接返回上次的订单，不重复扣库存
                TOrder previous = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                        .eq(TOrder::getOrderNo, exist));
                if (previous != null) {
                    return toCreateVO(previous, userId);
                }
            }
            if (!kv.setIfAbsent(tokenKey, "PENDING", Duration.ofMinutes(10))) {
                throw new BizException(ErrorCode.REPEAT_SUBMIT, "订单提交中，请稍后");
            }
        }

        try {
            OrderCreateVO vo = doCreate(userId, request);
            if (tokenKey != null) {
                kv.set(tokenKey, vo.getOrderNo(), Duration.ofMinutes(10));
            }
            return vo;
        } catch (RuntimeException e) {
            // 失败要放开幂等键，否则用户 10 分钟内不能重试
            if (tokenKey != null) {
                kv.delete(tokenKey);
            }
            throw e;
        }
    }

    private OrderCreateVO doCreate(Long userId, OrderCreateRequest request) {
        List<OrderLineDTO> lines = request.getLines();
        if (lines == null || lines.isEmpty()) {
            throw new BizException(ErrorCode.CART_EMPTY, "请选择要购买的商品");
        }
        Map<String, ProductSnapshot> snapshots = productService.snapshots(
                lines.stream().map(OrderLineDTO::getProductId).toList());

        List<PriceCalculator.PriceLine> priceLines = new ArrayList<>();
        List<ProductSnapshot> ordered = new ArrayList<>();
        List<Integer> qtys = new ArrayList<>();
        for (OrderLineDTO line : lines) {
            ProductSnapshot snapshot = snapshots.get(line.getProductId());
            if (snapshot == null) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (!snapshot.onSale()) {
                throw new BizException(ErrorCode.PRODUCT_OFF_SHELF, "「" + snapshot.name() + "」已下架");
            }
            int qty = line.getQty() == null || line.getQty() < 1 ? 1 : line.getQty();
            ordered.add(snapshot);
            qtys.add(qty);
            priceLines.add(new PriceCalculator.PriceLine(
                    snapshot.code(), snapshot.priceCent(), snapshot.listCent(), qty));
        }

        AddressVO address = addressService.getOwnedOrDefault(userId,
                CheckoutServiceImpl.parseId(request.getAddressId()));
        if (address == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请先添加收货地址");
        }

        long goodsCent = priceLines.stream().mapToLong(l -> l.priceCent() * l.qty()).sum();
        Long couponId = CheckoutServiceImpl.parseId(request.getCouponId());
        CouponService.CouponCheck coupon = couponService.check(couponId, userId, goodsCent);
        if (couponId != null && !coupon.usable()) {
            throw new BizException(ErrorCode.COUPON_UNUSABLE, coupon.reason());
        }
        PriceCalculator.PriceResult result = priceCalculator.calculate(priceLines, coupon.discountCent());

        String orderNo = nextOrderNo();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(tradeProperties.getOrderExpireMinutes());

        TOrder order = new TOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setPayStatus(0);
        order.setGoodsAmountCent(result.goodsCent());
        order.setDiscountCent(result.discountCent());
        order.setShipFeeCent(result.shipFeeCent());
        order.setPayAmountCent(result.payableCent());
        order.setCouponId(couponId);
        order.setReceiver(address.getReceiver());
        order.setPhone(address.getPhone());
        order.setAddressSnapshot(snapshotAddress(address));
        order.setDeliveryType(request.getDeliveryType() == null ? 1 : request.getDeliveryType());
        order.setRemark(request.getRemark());
        order.setReviewStatus(0);
        orderMapper.insert(order);

        // 扣库存：乐观锁，影响 0 行抛 30003，整个事务回滚
        for (int i = 0; i < ordered.size(); i++) {
            ProductSnapshot snapshot = ordered.get(i);
            stockService.deduct(snapshot.id(), qtys.get(i), orderNo);

            TOrderItem item = new TOrderItem();
            item.setOrderNo(orderNo);
            item.setProductId(snapshot.id());
            item.setProductCode(snapshot.code());
            item.setNameSnapshot(snapshot.name());
            item.setSpecSnapshot(snapshot.spec());
            item.setImageSnapshot(snapshot.image());
            item.setPriceCent(snapshot.priceCent());
            item.setListCent(snapshot.listCent());
            item.setQty(qtys.get(i));
            item.setTotalCent(snapshot.priceCent() * qtys.get(i));
            orderItemMapper.insert(item);
        }

        couponService.lock(couponId, userId, orderNo);
        writeLog(orderNo, 0, OrderStatus.UNPAID.getCode(), 1, userId, "订单创建");

        String payNo = paymentService.createForOrder(orderNo, userId, result.payableCent(), expireAt);

        if (Boolean.TRUE.equals(request.getFromCart())) {
            // 只有从购物车结算才删行，详情页「立即购买」不能动购物车
            cartService.removeLines(userId, ordered.stream().map(ProductSnapshot::code).toList());
        }

        messageService.sendTemplate(userId, MessageKind.ORDER, MessageTemplate.ORDER_CREATED, orderNo, orderNo);

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderNo(orderNo);
        vo.setPayNo(payNo);
        vo.setPayAmount(PriceUtil.toYuan(result.payableCent()));
        vo.setStatus(OrderStatus.UNPAID.getCode());
        vo.setExpireAt(expireAt.atZone(ZONE).toInstant().toEpochMilli());
        return vo;
    }

    @Override
    public PageResult<OrderBriefVO> list(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<TOrder> wrapper = new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getUserId, userId)
                .orderByDesc(TOrder::getCreatedAt)
                .orderByDesc(TOrder::getId);
        applyStatusFilter(wrapper, status);

        Page<TOrder> result = orderMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(toBriefList(result.getRecords()), result.getTotal(), page, size);
    }

    @Override
    public OrderDetailVO detail(Long userId, String orderNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo)
                .eq(TOrder::getUserId, userId));
        if (order == null) {
            // 不区分「不存在」和「不是你的」，避免探测别人的订单号
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        return toDetail(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO cancel(Long userId, String orderNo, OrderCancelRequest request) {
        TOrder order = requireOwnOrder(userId, orderNo);
        if (!Objects.equals(order.getStatus(), OrderStatus.UNPAID.getCode())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "只有待付款订单可以取消");
        }
        String reason = request == null || request.getReason() == null ? "用户取消" : request.getReason();
        order.setCancelReason(reason);
        order.setCloseTime(LocalDateTime.now());
        transit(order, OrderStatus.CANCELLED, 1, userId, reason);
        restoreStock(order, 2);
        couponService.releaseByOrder(orderNo);
        messageService.sendTemplate(userId, MessageKind.ORDER, MessageTemplate.ORDER_CANCELLED, orderNo, orderNo);
        return toDetail(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO confirm(Long userId, String orderNo) {
        TOrder order = requireOwnOrder(userId, orderNo);
        if (!Objects.equals(order.getStatus(), OrderStatus.UNRECEIVED.getCode())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "只有待收货订单可以确认收货");
        }
        order.setReceiveTime(LocalDateTime.now());
        transit(order, OrderStatus.FINISHED, 1, userId, "用户确认收货");
        return toDetail(order);
    }

    @Override
    public OrderCountVO counts(Long userId) {
        // 一条 GROUP BY 查完，不要发四条 count
        List<Map<String, Object>> rows = orderMapper.selectMaps(new QueryWrapper<TOrder>()
                .select("status", "review_status", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .groupBy("status", "review_status"));
        OrderCountVO vo = new OrderCountVO();
        int unpaid = 0;
        int shipping = 0;
        int review = 0;
        int after = 0;
        for (Map<String, Object> row : rows) {
            int status = Integer.parseInt(Objects.toString(row.get("status"), "0"));
            int reviewStatus = Integer.parseInt(Objects.toString(row.get("review_status"), "0"));
            int count = Integer.parseInt(Objects.toString(row.get("cnt"), "0"));
            switch (status) {
                case 10 -> unpaid += count;
                case 30 -> shipping += count;
                case 40 -> {
                    if (reviewStatus == 0) {
                        review += count;
                    }
                }
                case 60, 70 -> after += count;
                default -> {
                }
            }
        }
        vo.setUnpaid(unpaid);
        vo.setShipping(shipping);
        vo.setReview(review);
        vo.setAfter(after);
        return vo;
    }

    @Override
    public PageResult<OrderBriefVO> adminList(String status, String keyword, int page, int size) {
        LambdaQueryWrapper<TOrder> wrapper = new LambdaQueryWrapper<TOrder>()
                .orderByDesc(TOrder::getCreatedAt)
                .orderByDesc(TOrder::getId);
        applyStatusFilter(wrapper, status);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(TOrder::getOrderNo, keyword).or().like(TOrder::getReceiver, keyword));
        }
        Page<TOrder> result = orderMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(toBriefList(result.getRecords()), result.getTotal(), page, size);
    }

    @Override
    public OrderDetailVO adminDetail(String orderNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return toDetail(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO deliver(String orderNo, String company, String trackingNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), OrderStatus.UNDELIVERED.getCode())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "只有待发货订单可以发货");
        }
        order.setDeliverTime(LocalDateTime.now());
        String remark = "发货：" + (company == null ? "顺丰" : company) + " " + (trackingNo == null ? "" : trackingNo);
        transit(order, OrderStatus.UNRECEIVED, 3, null, remark);
        messageService.sendTemplate(order.getUserId(), MessageKind.ORDER,
                MessageTemplate.ORDER_DELIVERED, orderNo, orderNo);
        return toDetail(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders(int expireMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(expireMinutes);
        List<TOrder> expired = orderMapper.selectList(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getStatus, OrderStatus.UNPAID.getCode())
                .lt(TOrder::getCreatedAt, deadline));
        int count = 0;
        for (TOrder order : expired) {
            order.setCancelReason("超时未支付，系统自动关闭");
            order.setCloseTime(LocalDateTime.now());
            transit(order, OrderStatus.CANCELLED, 2, null, "超时未支付");
            restoreStock(order, 2);
            couponService.releaseByOrder(order.getOrderNo());
            messageService.sendTemplate(order.getUserId(), MessageKind.ORDER,
                    MessageTemplate.ORDER_CANCELLED, order.getOrderNo(), order.getOrderNo());
            count++;
        }
        return count;
    }

    // ---------- PayOrderPort：payment 定义的端口，由 trade 这一侧实现 ----------

    @Override
    public PayOrderInfo load(String orderNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo));
        if (order == null) {
            return null;
        }
        TOrderItem first = orderItemMapper.selectOne(new LambdaQueryWrapper<TOrderItem>()
                .eq(TOrderItem::getOrderNo, orderNo)
                .orderByAsc(TOrderItem::getId)
                .last("LIMIT 1"));
        return new PayOrderInfo(
                order.getOrderNo(),
                order.getUserId(),
                order.getStatus(),
                order.getPayStatus(),
                order.getPayAmountCent(),
                expireOf(order),
                first == null ? null : first.getNameSnapshot());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(String orderNo, String payNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (Objects.equals(order.getPayStatus(), 1)) {
            // 回调可能重放，已支付就当成功
            return;
        }
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        transit(order, OrderStatus.UNDELIVERED, 2, null, "支付成功 " + payNo);
        messageService.sendTemplate(order.getUserId(), MessageKind.ORDER, MessageTemplate.ORDER_PAID,
                orderNo, orderNo, PriceUtil.toYuan(order.getPayAmountCent()).toPlainString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRefunded(String orderNo, String payNo, String refundNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (Objects.equals(order.getStatus(), OrderStatus.REFUNDED.getCode())) {
            return;
        }
        // 已发货/已完成要先落到「退款中」再「已退款」，状态机不允许 20/30/40 直达 70
        if (!Objects.equals(order.getStatus(), OrderStatus.REFUNDING.getCode())) {
            transit(order, OrderStatus.REFUNDING, 2, null, "用户申请退款 " + refundNo);
        }
        order.setPayStatus(2);
        transit(order, OrderStatus.REFUNDED, 2, null, "退款到账 " + refundNo);
        // 退款和取消一样把货退回可售库存，流水 type 只有 1下单/2取消回补/3入库/4调整，用 2
        restoreStock(order, 2);
        messageService.sendTemplate(order.getUserId(), MessageKind.ORDER, MessageTemplate.ORDER_REFUNDED,
                orderNo, orderNo, PriceUtil.toYuan(order.getPayAmountCent()).toPlainString());
    }

    // ---------- 内部工具 ----------

    private void applyStatusFilter(LambdaQueryWrapper<TOrder> wrapper, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return;
        }
        switch (status) {
            case "unpaid" -> wrapper.eq(TOrder::getStatus, OrderStatus.UNPAID.getCode());
            case "shipping" -> wrapper.eq(TOrder::getStatus, OrderStatus.UNRECEIVED.getCode());
            case "review" -> wrapper.eq(TOrder::getStatus, OrderStatus.FINISHED.getCode())
                    .eq(TOrder::getReviewStatus, 0);
            case "after" -> wrapper.in(TOrder::getStatus,
                    OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode());
            case "undelivered" -> wrapper.eq(TOrder::getStatus, OrderStatus.UNDELIVERED.getCode());
            case "finished" -> wrapper.eq(TOrder::getStatus, OrderStatus.FINISHED.getCode());
            case "cancelled" -> wrapper.eq(TOrder::getStatus, OrderStatus.CANCELLED.getCode());
            case "refund" -> wrapper.in(TOrder::getStatus,
                    OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode());
            default -> {
                // 后台直接传数字状态码，如 20 待发货
                int code;
                try {
                    code = Integer.parseInt(status.trim());
                } catch (NumberFormatException e) {
                    return;
                }
                if (code > 0) {
                    wrapper.eq(TOrder::getStatus, code);
                }
            }
        }
    }

    /** 状态流转唯一入口：先校验 canTransitTo，再改状态、写流水 */
    private void transit(TOrder order, OrderStatus target, int operatorType, Long operatorId, String remark) {
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (!from.canTransitTo(target)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "订单当前是「" + from.getText() + "」，不能变成「" + target.getText() + "」");
        }
        order.setStatus(target.getCode());
        orderMapper.updateById(order);
        writeLog(order.getOrderNo(), from.getCode(), target.getCode(), operatorType, operatorId, remark);
    }

    private void writeLog(String orderNo, int fromStatus, int toStatus, int operatorType, Long operatorId, String remark) {
        TOrderStatusLog statusLog = new TOrderStatusLog();
        statusLog.setOrderNo(orderNo);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorType(operatorType);
        statusLog.setOperatorId(operatorId);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
    }

    private void restoreStock(TOrder order, int type) {
        List<TOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<TOrderItem>()
                .eq(TOrderItem::getOrderNo, order.getOrderNo()));
        for (TOrderItem item : items) {
            stockService.restore(item.getProductId(), item.getQty(), order.getOrderNo(), type);
        }
    }

    private TOrder requireOwnOrder(Long userId, String orderNo) {
        TOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TOrder>()
                .eq(TOrder::getOrderNo, orderNo)
                .eq(TOrder::getUserId, userId));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderCreateVO toCreateVO(TOrder order, Long userId) {
        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setPayNo(paymentService.findPayNoByOrder(order.getOrderNo()));
        vo.setPayAmount(PriceUtil.toYuan(order.getPayAmountCent()));
        vo.setStatus(order.getStatus());
        vo.setExpireAt(expireOf(order).atZone(ZONE).toInstant().toEpochMilli());
        return vo;
    }

    private List<OrderBriefVO> toBriefList(List<TOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<String> orderNos = orders.stream().map(TOrder::getOrderNo).toList();
        Map<String, List<TOrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<TOrderItem>().in(TOrderItem::getOrderNo, orderNos))
                .stream().collect(Collectors.groupingBy(TOrderItem::getOrderNo, LinkedHashMap::new, Collectors.toList()));

        List<OrderBriefVO> result = new ArrayList<>(orders.size());
        for (TOrder order : orders) {
            List<TOrderItem> items = itemsByOrder.getOrDefault(order.getOrderNo(), List.of());
            OrderBriefVO vo = new OrderBriefVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setStatusText(OrderStatus.textOf(order.getStatus()));
            vo.setPayAmount(PriceUtil.toYuan(order.getPayAmountCent()));
            vo.setTotalQty(items.stream().mapToInt(TOrderItem::getQty).sum());
            if (!items.isEmpty()) {
                vo.setFirstItemName(items.get(0).getNameSnapshot());
                vo.setCoverImage(items.get(0).getImageSnapshot());
            }
            vo.setCreatedAt(order.getCreatedAt() == null ? null
                    : order.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli());
            if (Objects.equals(order.getStatus(), OrderStatus.UNPAID.getCode())) {
                vo.setExpireAt(expireOf(order).atZone(ZONE).toInstant().toEpochMilli());
            }
            result.add(vo);
        }
        return result;
    }

    private OrderDetailVO toDetail(TOrder order) {
        List<TOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<TOrderItem>()
                .eq(TOrderItem::getOrderNo, order.getOrderNo()));
        List<TOrderStatusLog> logs = statusLogMapper.selectList(new LambdaQueryWrapper<TOrderStatusLog>()
                .eq(TOrderStatusLog::getOrderNo, order.getOrderNo())
                .orderByAsc(TOrderStatusLog::getId));

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatus.textOf(order.getStatus()));
        vo.setPayStatus(order.getPayStatus());

        AmountVO amounts = new AmountVO();
        amounts.setGoodsAmount(PriceUtil.toYuan(order.getGoodsAmountCent()));
        amounts.setDiscount(PriceUtil.toYuan(order.getDiscountCent()));
        amounts.setShipFee(PriceUtil.toYuan(order.getShipFeeCent()));
        amounts.setPayable(PriceUtil.toYuan(order.getPayAmountCent()));
        amounts.setSaved(PriceUtil.toYuan(items.stream()
                .mapToLong(i -> Math.max(0L, i.getListCent() - i.getPriceCent()) * i.getQty()).sum()));
        vo.setAmounts(amounts);

        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getProductCode());
            itemVO.setName(item.getNameSnapshot());
            itemVO.setSpec(item.getSpecSnapshot());
            itemVO.setImage(item.getImageSnapshot());
            itemVO.setPrice(PriceUtil.toYuan(item.getPriceCent()));
            itemVO.setList(PriceUtil.toYuan(item.getListCent()));
            itemVO.setQty(item.getQty());
            itemVO.setTotal(PriceUtil.toYuan(item.getTotalCent()));
            return itemVO;
        }).toList());

        vo.setAddress(addressOf(order));
        vo.setDeliveryType(order.getDeliveryType());
        vo.setDeliveryText(deliveryText(order.getDeliveryType()));
        vo.setRemark(order.getRemark());
        vo.setPayNo(paymentService.findPayNoByOrder(order.getOrderNo()));
        vo.setCreatedAt(toMillis(order.getCreatedAt()));
        vo.setPayTime(toMillis(order.getPayTime()));
        vo.setDeliverTime(toMillis(order.getDeliverTime()));
        vo.setReceiveTime(toMillis(order.getReceiveTime()));
        vo.setCloseTime(toMillis(order.getCloseTime()));
        vo.setCancelReason(order.getCancelReason());

        vo.setTimeline(logs.stream().map(log -> {
            TimelineVO timeline = new TimelineVO();
            timeline.setStatus(log.getToStatus());
            timeline.setStatusText(OrderStatus.textOf(log.getToStatus()));
            timeline.setAt(toMillis(log.getCreatedAt()));
            timeline.setRemark(log.getRemark());
            return timeline;
        }).toList());
        return vo;
    }

    /** 订单里展示的地址独立于地址簿：改地址不影响历史订单，手机号打码 */
    private AddressVO addressOf(TOrder order) {
        AddressVO vo = new AddressVO();
        Map<String, Object> snapshot = order.getAddressSnapshot();
        if (snapshot != null) {
            vo.setId(str(snapshot.get("id")));
            vo.setProvince(str(snapshot.get("province")));
            vo.setCity(str(snapshot.get("city")));
            vo.setDistrict(str(snapshot.get("district")));
            vo.setDetail(str(snapshot.get("detail")));
            vo.setTag(str(snapshot.get("tag")));
        }
        vo.setReceiver(order.getReceiver());
        vo.setPhone(IdUtils.maskPhone(order.getPhone()));
        vo.setIsDefault(false);
        return vo;
    }

    private Map<String, Object> snapshotAddress(AddressVO address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", address.getId());
        snapshot.put("receiver", address.getReceiver());
        snapshot.put("phone", address.getPhone());
        snapshot.put("province", address.getProvince());
        snapshot.put("city", address.getCity());
        snapshot.put("district", address.getDistrict());
        snapshot.put("detail", address.getDetail());
        snapshot.put("tag", address.getTag());
        return snapshot;
    }

    private String deliveryText(Integer type) {
        if (type == null) {
            return "极速达 · 最快 1 小时送达";
        }
        return switch (type) {
            case 2 -> "次日达 · 次日 18:00 前送达";
            case 3 -> "自提 · 门店营业时间内自取";
            default -> "极速达 · 最快 1 小时送达";
        };
    }

    private LocalDateTime expireOf(TOrder order) {
        LocalDateTime createdAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();
        return createdAt.plusMinutes(tradeProperties.getOrderExpireMinutes());
    }

    private Long toMillis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** OWL + yyyyMMdd + 6 位当天序列（Redis INCR，当天自动从 1 开始） */
    private String nextOrderNo() {
        String day = LocalDateTime.now().format(DAY);
        String prefix = "OWL" + day;
        String key = RedisKey.orderSeq(day);
        // 没起 Redis 时序号存在进程内，重启会归零，先跟库里当天最大号对齐，否则撞 uk_order_no
        if (!kv.exists(key)) {
            Integer max = orderMapper.maxSeqOfPrefix(prefix);
            if (max != null && max > 0) {
                kv.setIfAbsent(key, String.valueOf(max), Duration.ofDays(2));
            }
        }
        long seq = kv.increment(key, Duration.ofDays(2));
        return prefix + String.format("%06d", seq);
    }
}
