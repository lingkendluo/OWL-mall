package com.owl.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_status_log")
public class TOrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Integer fromStatus;

    private Integer toStatus;

    /** 1 用户 2 系统 3 管理员 */
    private Integer operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createdAt;
}
