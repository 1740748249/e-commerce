package com.ecommerce.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "ecommerce.jwt")
@Data
public class JwtProperties {
    private Resource location;
    private String alias;
    private String password;
    private String tokenTTL;
}
