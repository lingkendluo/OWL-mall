package com.owl.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("p_refund")
public class PRefund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String payNo;

    private String orderNo;

    private Long userId;

    private Long amountCent;

    private String reason;

    /** 0 处理中 1 成功 2 失败 */
    private Integer status;

    private String channelRefundNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
