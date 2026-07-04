package com.ecommerce.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class CouponRedisConfig {

    @Bean
    public DefaultRedisScript<Long> claimCouponScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/claim_coupon.lua")));
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> initCouponMetaScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/init_coupon_meta.lua")));
        return script;
    }
}
