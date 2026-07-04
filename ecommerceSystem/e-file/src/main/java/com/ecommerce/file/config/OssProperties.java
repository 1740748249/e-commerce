package com.ecommerce.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "ecommerce.file.oss")
public class OssProperties {
    private String endpoint;
    private String bucketName;
    private String domain;

    private String accessKeyId;
    private String accessKeySecret;

    @PostConstruct
    public void loadFromEnv() {
        this.accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        this.accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        if (accessKeyId == null || accessKeySecret == null) {
            throw new IllegalStateException(
                    "OSS 环境变量未设置，请配置 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET");
        }
    }
}
