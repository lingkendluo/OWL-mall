package com.owl.common.api;

import lombok.Getter;

/**
 * 业务错误码，5 位数字，见规格书附录 A。
 * httpStatus 只给少数几个用（401/403/429/500），其余业务失败一律 200。
 */
@Getter
public enum ErrorCode {

    OK(0, "ok", 200),

    PARAM_ERROR(10001, "参数错误", 200),
    UNAUTHORIZED(10002, "未登录或登录已过期", 401),
    FORBIDDEN(10003, "无权限", 403),
    NOT_FOUND(10004, "资源不存在", 200),
    REPEAT_SUBMIT(10005, "重复提交", 200),
    TOO_FREQUENT(10006, "请求过于频繁", 429),

    LOGIN_FAILED(20001, "账号或密码错误", 200),
    SMS_CODE_ERROR(20002, "验证码错误或已过期", 200),
    PHONE_REGISTERED(20003, "手机号已注册", 200),
    ACCOUNT_DISABLED(20004, "账号已被禁用", 200),
    LOGIN_LOCKED(20005, "尝试次数过多，请稍后再试", 200),

    PRODUCT_NOT_FOUND(30001, "商品或类目不存在", 200),
    PRODUCT_OFF_SHELF(30002, "商品已下架", 200),
    STOCK_NOT_ENOUGH(30003, "库存不足", 200),
    CATEGORY_NOT_EMPTY(30004, "该类目下仍有商品，不能删除", 200),

    CART_EMPTY(40001, "购物车为空", 200),
    ORDER_NOT_FOUND(40002, "订单不存在", 200),
    ORDER_STATUS_INVALID(40003, "订单状态不允许此操作", 200),
    ORDER_EXPIRED(40004, "订单已超时关闭", 200),
    COUPON_UNUSABLE(40005, "优惠券不可用", 200),

    PAY_NOT_FOUND(50001, "支付单不存在或参数错误", 200),
    PAY_STATUS_INVALID(50002, "支付状态异常", 200),
    REFUND_DUPLICATE(50003, "退款申请重复或金额超限", 200),

    MESSAGE_NOT_FOUND(60001, "消息不存在", 200),
    NOTICE_NOT_FOUND(60002, "公告不存在", 200),

    AI_UNAVAILABLE(70001, "AI 服务暂时不可用", 200),
    AI_LIMIT(70002, "AI 调用次数已达上限", 200),

    SYSTEM_ERROR(90001, "系统内部错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public static ErrorCode of(int code) {
        for (ErrorCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return SYSTEM_ERROR;
    }
}
