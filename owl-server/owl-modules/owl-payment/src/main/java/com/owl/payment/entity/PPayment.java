package com.owl.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("p_payment")
public class PPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String orderNo;

    private Long userId;

    /** mock/wechat/alipay */
    private String channel;

    private Long amountCent;

    /** 0 待支付 1 成功 2 失败 3 关闭 4 已退款 */
    private Integer status;

    private String outTradeNo;

    private LocalDateTime paidAt;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
