package com.owl.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("c_category")
public class CCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外 id，如 snacks */
    private String code;

    private String name;

    private String hint;

    /** 内联 SVG path，前端用 v-html 渲染，原样返回不要转义 */
    private String glyph;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
