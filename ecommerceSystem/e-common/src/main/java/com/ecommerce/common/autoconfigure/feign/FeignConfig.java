package com.ecommerce.common.autoconfigure.feign;

import com.ecommerce.common.utils.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignConfig {

    @Bean
    public RequestInterceptor userInfoFeignInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userId = UserContext.getUserId();
                if (userId != null) {
                    template.header("user-info", userId.toString());
                }
                Long shopId = UserContext.getShopId();
                if (shopId != null) {
                    template.header("X-Shop-Id", shopId.toString());
                }
                Integer role = UserContext.getRole();
                if (role != null) {
                    template.header("X-User-Role", role.toString());
                }
            }
        };
    }
}
