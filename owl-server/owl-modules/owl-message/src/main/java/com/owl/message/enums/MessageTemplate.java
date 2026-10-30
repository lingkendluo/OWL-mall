package com.owl.message.enums;

/**
 * 消息模板。业务代码只传参数，文案集中在这里，改文案不用翻业务代码。
 */
public enum MessageTemplate {

    ORDER_CREATED("订单已创建", "订单 %s 已创建，请在 30 分钟内完成支付。"),
    ORDER_PAID("支付成功", "订单 %s 已支付 ¥%s，我们正在为你拣货。"),
    ORDER_DELIVERED("订单已发出", "订单 %s 已由杭州文三路店拣货完成，骑手正在路上，预计 1 小时内送达。"),
    ORDER_CANCELLED("订单已取消", "订单 %s 已取消，库存与优惠券已退回。"),
    ORDER_REFUNDED("退款已到账", "订单 %s 的退款 ¥%s 已原路退回，请留意到账通知。"),
    WELCOME("欢迎加入 OWL", "欢迎你，%s！新人券已放进「我的券」，满 99 减 10，随时可用。");

    private final String title;
    private final String body;

    MessageTemplate(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String title() {
        return title;
    }

    public String body(Object... args) {
        return String.format(body, args);
    }
}
