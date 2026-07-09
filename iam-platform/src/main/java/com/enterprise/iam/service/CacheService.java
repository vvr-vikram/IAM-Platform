package com.enterprise.iam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();
    private boolean useLocalFallback = false;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static class LocalCacheEntry {
        final Object value;
        final Instant expiry;

        LocalCacheEntry(Object value, Duration ttl) {
            this.value = value;
            this.expiry = Instant.now().plus(ttl);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (!useLocalFallback) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                return;
            } catch (Exception e) {
                log.warn("Redis operations failed. Switching to local in-memory cache fallback. Error: {}", e.getMessage());
                useLocalFallback = true;
            }
        }
        localCache.put(key, new LocalCacheEntry(value, ttl));
    }

    public Object get(String key) {
        if (!useLocalFallback) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                log.warn("Redis operations failed. Switching to local in-memory cache fallback. Error: {}", e.getMessage());
                useLocalFallback = true;
            }
        }
        
        LocalCacheEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            localCache.remove(key);
            return null;
        }
        return entry.value;
    }

    public void remove(String key) {
        if (!useLocalFallback) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                log.warn("Redis operations failed. Switching to local in-memory cache fallback. Error: {}", e.getMessage());
                useLocalFallback = true;
            }
        }
        localCache.remove(key);
    }

    public boolean hasKey(String key) {
        if (!useLocalFallback) {
            try {
                Boolean hasKey = redisTemplate.hasKey(key);
                return hasKey != null && hasKey;
            } catch (Exception e) {
                log.warn("Redis operations failed. Switching to local in-memory cache fallback. Error: {}", e.getMessage());
                useLocalFallback = true;
            }
        }
        
        LocalCacheEntry entry = localCache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            localCache.remove(key);
            return false;
        }
        return true;
    }
}
