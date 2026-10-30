package com.owl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.owl")
@MapperScan("com.owl.**.mapper")
@ConfigurationPropertiesScan("com.owl")
@EnableScheduling
public class OwlApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwlApplication.class, args);
    }
}
