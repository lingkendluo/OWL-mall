package com.owl.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建订单")
public class OrderCreateRequest {

    @NotEmpty(message = "请选择要购买的商品")
    private List<OrderLineDTO> lines;

    private String addressId;

    private String couponId;

    private String remark;

    /** 1 极速达 2 次日达 3 自提 */
    private Integer deliveryType = 1;

    /** 从购物车「去结算」进来传 true；详情页「立即购买」传 false，否则会误删购物车同款 */
    private Boolean fromCart = Boolean.FALSE;

    /** 幂等键，同一 token 重复提交返回同一订单 */
    private String clientToken;
}
