package com.owl.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("c_sub_category")
public class CSubCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String code;

    private String name;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
