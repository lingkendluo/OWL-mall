package com.owl.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "t_order", autoResultMap = true)
public class TOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 10 待付款 20 待发货 30 待收货 40 已完成 50 已取消 60 退款中 70 已退款 */
    private Integer status;

    /** 0 未支付 1 已支付 2 已退款 */
    private Integer payStatus;

    private Long goodsAmountCent;

    private Long discountCent;

    private Long shipFeeCent;

    private Long payAmountCent;

    private Long couponId;

    private String receiver;

    private String phone;

    /** 下单时的地址快照，商品改价、地址改址都不影响历史订单 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> addressSnapshot;

    /** 1 极速达 2 次日达 3 自提 */
    private Integer deliveryType;

    private String remark;

    private Integer reviewStatus;

    private LocalDateTime payTime;

    private LocalDateTime deliverTime;

    private LocalDateTime receiveTime;

    private LocalDateTime closeTime;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
