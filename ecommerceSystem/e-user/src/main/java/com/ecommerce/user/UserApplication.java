package com.ecommerce.user;

import com.ecommerce.user.config.AuthProperties;
import com.ecommerce.user.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.ecommerce.user.mapper")
@EnableFeignClients(basePackages = "com.ecommerce.api.client")
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
