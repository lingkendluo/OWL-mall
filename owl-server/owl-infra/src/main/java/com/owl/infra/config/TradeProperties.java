package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owl.trade")
public class TradeProperties {

    /** 未支付订单存活分钟数，超时自动关单 */
    private int orderExpireMinutes = 30;

    /** 单笔订单单行最大件数，与前端 MAX_QTY 对齐 */
    private int maxQty = 99;
}
