package com.moforum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final String PUBLIC_PREFIX = "/uploads/";

    private final String rootPath;

    public LocalStorageService(@Value("${storage.local.path:/app/uploads}") String rootPath) {
        this.rootPath = rootPath;
    }

    public String upload(MultipartFile file) {
        String ext = "";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            ext = name.substring(name.lastIndexOf("."));
        }
        String key = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(rootPath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(key));
            return PUBLIC_PREFIX + key;
        } catch (IOException e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    public void delete(String url) {
        deleteByKey(extractKey(url));
    }

    public void deleteByKey(String key) {
        if (!isValidKey(key)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(rootPath, key));
        } catch (IOException e) {
            // 删除失败不影响主流程
        }
    }

    public String extractKey(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) {
            return null;
        }
        String key = url.substring(PUBLIC_PREFIX.length());
        return isValidKey(key) ? key : null;
    }

    private static boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return !key.contains("/") && !key.contains("\\") && !key.contains("..");
    }
}
