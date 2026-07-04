package com.ecommerce.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ecommerce.file.config.OssProperties;
import com.ecommerce.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class OssStorageServiceImpl implements FileStorageService {

    private final OssProperties ossProperties;
    private OSS client;

    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    ));

    public OssStorageServiceImpl(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @PostConstruct
    public void init() {
        client = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Override
    public String upload(MultipartFile file, String type) {
        if (file.isEmpty()) {
            log.error("文件为空");
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = FileUtil.extName(originalFilename);
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式: " + ext);
        }

        // OSS 路径: avatar/uuid.jpg
        String objectKey = type + "/" + IdUtil.simpleUUID() + "." + ext;

        try {

            client.putObject(ossProperties.getBucketName(), objectKey, file.getInputStream());
        } catch (IOException e) {
            log.error("文件上传到 OSS 错误{e}", e);
            throw new RuntimeException("文件上传到 OSS 失败", e);
        }

        // 返回完整 URL: https://bucket.endpoint.com/avatar/uuid.jpg
        log.info("文件上传成功:{}", ossProperties.getDomain() + "/" + objectKey);
        return ossProperties.getDomain() + "/" + objectKey;
    }
}
