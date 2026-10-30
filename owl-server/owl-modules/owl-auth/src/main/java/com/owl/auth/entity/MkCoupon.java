package com.owl.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mk_coupon")
public class MkCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    /** 1 满减 2 折扣 */
    private Integer type;

    private Long thresholdCent;

    private Long discountCent;

    /** 85 = 8.5 折 */
    private Integer discountRate;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer total;

    private Integer received;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
