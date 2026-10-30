package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owl.jwt")
public class JwtProperties {

    /** 至少 32 字节，生产必须走环境变量 OWL_JWT_SECRET */
    private String secret = "owl-mall-dev-secret-please-change-in-prod-32chars";

    /** access token 有效期，秒 */
    private long accessTtl = 7200;

    /** refresh token 有效期，秒 */
    private long refreshTtl = 1209600;
}
