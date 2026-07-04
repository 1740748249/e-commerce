package com.ecommerce.api.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 传递网关来源标记，避免 Feign 调用触发响应包装
            requestTemplate.header("x-request-from", "feign");
        };
    }
}
