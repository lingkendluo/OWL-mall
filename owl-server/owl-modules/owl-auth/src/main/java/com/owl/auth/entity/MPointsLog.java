package com.owl.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m_points_log")
public class MPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 正加负减 */
    private Integer changeVal;

    /** 1 下单 2 评价 3 退款扣回 4 后台调整 */
    private Integer type;

    private String refNo;

    private String remark;

    private LocalDateTime createdAt;
}
