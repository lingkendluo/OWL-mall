package com.owl.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("c_product")
public class CProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外 id，如 snacks-1 */
    private String code;

    private Long categoryId;

    private Long subId;

    private String name;

    private String spec;

    /** 售价，分 */
    private Long priceCent;

    /** 划线价，分 */
    private Long listCent;

    private String tag;

    private String emoji;

    private String image;

    private Integer stock;

    private Integer sales;

    private Integer sort;

    private Integer isFeatured;

    /** 1 上架 0 下架 */
    private Integer status;

    /**
     * 乐观锁版本号。故意不挂 @Version：扣库存用自定义 CAS SQL，
     * 挂上注解会让 updateById 也带上版本条件，后台改价的场景会莫名失败。
     */
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
