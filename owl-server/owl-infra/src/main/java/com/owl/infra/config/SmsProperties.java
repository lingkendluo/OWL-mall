package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owl.sms")
public class SmsProperties {

    /** dev 环境把验证码回传，方便 curl 联调；prod 必须 false */
    private boolean returnCode = false;

    /** 同一手机号重发间隔，秒 */
    private int resendSeconds = 60;
}
