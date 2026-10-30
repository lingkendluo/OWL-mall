package com.owl.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s_admin_log")
public class SAdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    /** PRODUCT_UPDATE / ORDER_DELIVER ... */
    private String action;

    private String target;

    private String detail;

    private String ip;

    private LocalDateTime createdAt;
}
