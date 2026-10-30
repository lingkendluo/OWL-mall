package com.owl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增/修改收货地址")
public class AddressRequest {

    @NotBlank(message = "请输入收货人")
    @Size(max = 32, message = "收货人最多 32 个字符")
    private String receiver;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请选择省份")
    private String province;

    @NotBlank(message = "请选择城市")
    private String city;

    @NotBlank(message = "请选择区县")
    private String district;

    @NotBlank(message = "请输入详细地址")
    @Size(max = 128, message = "详细地址最多 128 个字符")
    private String detail;

    private String tag;

    private Boolean isDefault = Boolean.FALSE;
}
