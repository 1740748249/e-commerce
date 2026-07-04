package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /** 基于请求 IP 的限流，秒杀接口使用 */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Objects.requireNonNullElse(
                    exchange.getRequest().getRemoteAddress(),
                    exchange.getRequest().getRemoteAddress())
                    .getAddress()
                    .getHostAddress();
            return Mono.just(ip);
        };
    }
}
