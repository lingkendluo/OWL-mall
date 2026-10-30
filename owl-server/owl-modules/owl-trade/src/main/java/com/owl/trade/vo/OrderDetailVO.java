package com.owl.trade.vo;

import com.owl.auth.vo.AddressVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "订单详情")
public class OrderDetailVO {

    private String orderNo;

    private Integer status;

    private String statusText;

    private Integer payStatus;

    private AmountVO amounts;

    private List<OrderItemVO> items;

    @Schema(description = "手机号已打码")
    private AddressVO address;

    private Integer deliveryType;

    private String deliveryText;

    private String remark;

    private String payNo;

    private Long createdAt;

    private Long payTime;

    private Long deliverTime;

    private Long receiveTime;

    private Long closeTime;

    private String cancelReason;

    private List<TimelineVO> timeline;
}
