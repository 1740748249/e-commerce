package com.ecommerce.gateway.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "ecommerce.jwt")
@Data
public class JwtProperties {
    private Resource location; //密钥所在文件
    private String alias; //密钥文件别名
    private String password; //密钥文件密码
    private String tokenTTL; //登录有效期
}
