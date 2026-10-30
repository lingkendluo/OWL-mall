package com.owl.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("c_stock_log")
public class CStockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 负数为扣减 */
    private Integer changeQty;

    /** 1 下单 2 取消回补 3 入库 4 后台调整 */
    private Integer type;

    private String refNo;

    private Integer beforeQty;

    private Integer afterQty;

    private LocalDateTime createdAt;
}
