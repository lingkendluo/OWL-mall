package com.owl.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("n_message")
public class NMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** order/promo/system，直接对齐前端 kind */
    private String kind;

    private String title;

    private String body;

    private String bizNo;

    private Integer readFlag;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
