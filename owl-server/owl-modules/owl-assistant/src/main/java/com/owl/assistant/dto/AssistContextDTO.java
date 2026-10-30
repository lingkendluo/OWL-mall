package com.owl.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 前端实时算好的购物车快照。后端不重新查库：这些数字就是用户屏幕上看到的，
 * 助手回答与页面必须一致，查库反而可能因为缓存/并发对不上。
 */
@Data
@Schema(description = "购物车上下文")
public class AssistContextDTO {

    private Integer cartCount;

    private BigDecimal cartAmount;

    private BigDecimal cartCheckedAmount;

    private BigDecimal cartSaved;

    /** 还差多少免运费，已免则为 0 */
    private BigDecimal toFreeShipping;

    private Boolean isFreeShipping;

    private BigDecimal freeShippingAt;

    private List<AssistLineDTO> lines;
}
