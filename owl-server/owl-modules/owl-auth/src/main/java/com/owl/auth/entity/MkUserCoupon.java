package com.owl.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mk_user_coupon")
public class MkUserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    /** 0 未用 1 已用 2 过期 */
    private Integer status;

    private String orderNo;

    private LocalDateTime receivedAt;

    private LocalDateTime usedAt;
}
