package com.moforum.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${oss.endpoint:}")
    private String endpoint;

    @Value("${oss.access-key:}")
    private String accessKey;

    @Value("${oss.secret-key:}")
    private String secretKey;

    @Bean
    public OSS ossClient() {
        if (endpoint.isEmpty() || accessKey.isEmpty() || secretKey.isEmpty()) {
            return null;
        }
        return new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }
}
