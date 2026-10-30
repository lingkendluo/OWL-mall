package com.owl.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owl.ai")
public class AiProperties {

    private String baseUrl = "https://api.deepseek.com";
    /** 只从环境变量 DEEPSEEK_API_KEY 读，yml 里留空 */
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutMs = 30000;
    private double temperature = 0.6;
    private int maxTokens = 600;
}
