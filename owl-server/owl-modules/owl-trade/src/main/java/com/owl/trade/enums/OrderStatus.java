package com.owl.trade.enums;

import lombok.Getter;

import java.util.Set;

/**
 * 订单状态机。所有状态流转必须走 canTransitTo，禁止到处 setStatus。
 *
 * <pre>
 * 10 待付款 ──支付成功──> 20 待发货 ──发货──> 30 待收货 ──确认──> 40 已完成
 *    │                        │                    │
 *    │取消/超时                │退款                 │退款
 *    ▼                        ▼                    ▼
 * 50 已取消              60 退款中 ──退款成功──> 70 已退款
 * </pre>
 */
@Getter
public enum OrderStatus {

    UNPAID(10, "待付款"),
    UNDELIVERED(20, "待发货"),
    UNRECEIVED(30, "待收货"),
    FINISHED(40, "已完成"),
    CANCELLED(50, "已取消"),
    REFUNDING(60, "退款中"),
    REFUNDED(70, "已退款");

    private final int code;
    private final String text;

    OrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }

    public static String textOf(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status.text;
            }
        }
        return "";
    }

    /** 允许流转的目标，其它一律 40003 */
    public boolean canTransitTo(OrderStatus target) {
        return switch (this) {
            case UNPAID -> Set.of(CANCELLED, UNDELIVERED).contains(target);
            case UNDELIVERED -> Set.of(UNRECEIVED, REFUNDING).contains(target);
            case UNRECEIVED -> Set.of(FINISHED, REFUNDING).contains(target);
            case FINISHED -> Set.of(REFUNDING).contains(target);
            case REFUNDING -> Set.of(REFUNDED).contains(target);
            case CANCELLED, REFUNDED -> false;
        };
    }
}
