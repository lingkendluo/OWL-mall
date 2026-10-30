package com.owl.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_item")
public class TOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private String productCode;

    private String nameSnapshot;

    private String specSnapshot;

    private String imageSnapshot;

    private Long priceCent;

    private Long listCent;

    private Integer qty;

    private Long totalCent;

    private LocalDateTime createdAt;
}
