package com.owl.infra.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档信息，访问 /doc.html。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI owlOpenApi() {
        return new OpenAPI().info(new Info()
                .title("OWL 商城 API")
                .description("C 端 /api/v1，B 端 /api/v1/admin")
                .version("1.0.0"));
    }
}
