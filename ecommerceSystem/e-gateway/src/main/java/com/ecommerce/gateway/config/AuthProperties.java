package com.ecommerce.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ecommerce.auth")
@Data
public class AuthProperties {
    private List<String> excludePaths;
    private List<String> includePaths;

    /** 路径模式 → 允许的角色列表. 例: /admin/** → [2], /notifications/** → [1,2] */
    private Map<String, List<Integer>> rolePaths;
}
