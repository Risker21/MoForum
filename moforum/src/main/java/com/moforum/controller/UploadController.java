package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.service.LocalStorageService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_SIZE = 3 * 1024 * 1024;

    private final LocalStorageService storageService;
    private final StringRedisTemplate redis;

    public UploadController(LocalStorageService storageService, StringRedisTemplate redis) {
        this.storageService = storageService;
        this.redis = redis;
    }

    @PostMapping("/image")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        if (file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Map.of("success", false, "message", "图片大小不能超过 3MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Map.of("success", false, "message", "仅支持图片格式");
        }
        try {
            String url = storageService.upload(file);
            String key = storageService.extractKey(url);
            if (key != null) {
                redis.opsForSet().add("img:pending", key);
                redis.opsForHash().put("img:pending:meta", key, String.valueOf(System.currentTimeMillis()));
            }
            return Map.of("success", true, "message", "上传成功", "url", url);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirmImage(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        String url = body.get("url");
        if (url == null || url.isEmpty()) {
            return Map.of("success", false, "message", "参数缺失");
        }
        String key = storageService.extractKey(url);
        if (key != null) {
            redis.opsForSet().add("img:confirmed", key);
            redis.opsForSet().remove("img:pending", key);
            redis.opsForHash().delete("img:pending:meta", key);
        }
        return Map.of("success", true);
    }
}
