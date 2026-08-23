package com.moforum.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EnableScheduling
public class ImageCleanupService {

    private final LocalStorageService storageService;
    private final StringRedisTemplate redis;

    public ImageCleanupService(LocalStorageService storageService, StringRedisTemplate redis) {
        this.storageService = storageService;
        this.redis = redis;
    }

    @Scheduled(fixedRate = 600_000)
    public void cleanupOrphanedImages() {
        Set<String> pending = redis.opsForSet().members("img:pending");
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Set<String> confirmed = redis.opsForSet().members("img:confirmed");
        long cutoff = System.currentTimeMillis() - 30 * 60 * 1000L;
        for (String key : pending) {
            String ts = (String) redis.opsForHash().get("img:pending:meta", key);
            if (ts == null) {
                continue;
            }
            try {
                if (Long.parseLong(ts) < cutoff && (confirmed == null || !confirmed.contains(key))) {
                    storageService.deleteByKey(key);
                    redis.opsForSet().remove("img:pending", key);
                    redis.opsForHash().delete("img:pending:meta", key);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
