package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "收货地址")
public class AddressVO {

    @Schema(description = "地址 id，字符串")
    private String id;

    private String receiver;

    @Schema(description = "本人查看自己的地址返回真实手机号；订单里展示才打码")
    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private String tag;

    private Boolean isDefault;
}
