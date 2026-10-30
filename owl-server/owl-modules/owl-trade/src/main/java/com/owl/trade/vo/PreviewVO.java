package com.owl.trade.vo;

import com.owl.auth.vo.AddressVO;
import com.owl.auth.vo.CouponBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "结算预览")
public class PreviewVO {

    private List<PreviewItemVO> items;

    private AmountVO amounts;

    private ShippingVO shipping;

    private AddressVO address;

    private List<CouponBriefVO> coupons;

    @Schema(description = "库存不足等提示，不阻断预览")
    private List<String> warnings;
}
