package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 运费规则。前端 FREE_SHIPPING=99 / SHIP_FEE=5 必须与这里对得上，禁止在下单逻辑里硬编码。
 */
@Data
@ConfigurationProperties(prefix = "owl.shipping")
public class ShippingProperties {

    private long freeThresholdCent = 9900L;
    private long feeCent = 500L;
}
