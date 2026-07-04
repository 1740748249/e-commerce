package com.ecommerce.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ecommerce.auth")
@Data
public class AuthProperties {
    private List<String> excludePaths;
    private List<String> includePaths;
}
