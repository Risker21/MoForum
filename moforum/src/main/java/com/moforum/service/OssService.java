package com.moforum.service;

import com.aliyun.oss.OSS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class OssService {

    @Autowired(required = false)
    private OSS ossClient;

    @Value("${oss.bucket:moforum-images}")
    private String bucket;

    @Value("${oss.endpoint:}")
    private String endpoint;

    public boolean isEnabled() {
        return ossClient != null && !endpoint.isEmpty();
    }

    public String upload(MultipartFile file) {
        if (!isEnabled()) {
            throw new RuntimeException("OSS 未配置");
        }
        String ext = "";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            ext = name.substring(name.lastIndexOf("."));
        }
        String key = "uploads/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            ossClient.putObject(bucket, key, file.getInputStream());
            return "https://" + bucket + "." + endpoint.replace("https://", "") + "/" + key;
        } catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    public void delete(String url) {
        if (!isEnabled() || url == null || url.isEmpty()) {
            return;
        }
        String key = extractKey(url);
        if (key != null) {
            ossClient.deleteObject(bucket, key);
        }
    }

    public void deleteByKey(String key) {
        if (isEnabled() && key != null) {
            ossClient.deleteObject(bucket, key);
        }
    }

    public String extractKey(String url) {
        String prefix = "https://" + bucket + "." + endpoint.replace("https://", "") + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        return null;
    }
}
