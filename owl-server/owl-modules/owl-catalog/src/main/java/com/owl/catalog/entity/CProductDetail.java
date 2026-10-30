package com.owl.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品详情四段。facts 用 LinkedHashMap 保序，前端按插入顺序渲染。
 */
@Data
@TableName(value = "c_product_detail", autoResultMap = true)
public class CProductDetail {

    @TableId(value = "product_id", type = IdType.INPUT)
    private Long productId;

    private String slogan;

    private String intro;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> factsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tipsJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
