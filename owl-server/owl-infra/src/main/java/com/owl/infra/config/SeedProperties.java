package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owl.seed")
public class SeedProperties {

    /** 启动时是否灌种子数据，生产置 false */
    private boolean enabled = true;
}
