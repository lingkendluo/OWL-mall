package com.owl.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 本地缓存（Caffeine）。每条自带 TTL，不用为不同 TTL 建多个 Cache 实例。
 * 单体阶段业务缓存只走这里，Redis 只放短生命周期的状态键。
 */
@Component
public class CacheService {

    private record Item(Object value, long expireAtNanos) {
    }

    private final Cache<String, Item> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfter(new Expiry<String, Item>() {
                @Override
                public long expireAfterCreate(String key, Item item, long currentTime) {
                    return Math.max(0L, item.expireAtNanos() - currentTime);
                }

                @Override
                public long expireAfterUpdate(String key, Item item, long currentTime, long currentDuration) {
                    return Math.max(0L, item.expireAtNanos() - currentTime);
                }

                @Override
                public long expireAfterRead(String key, Item item, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    public void put(String key, Object value, Duration ttl) {
        cache.put(key, new Item(value, System.nanoTime() + ttl.toNanos()));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Item item = cache.getIfPresent(key);
        if (item == null) {
            return null;
        }
        return type.isInstance(item.value()) ? (T) item.value() : null;
    }

    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        Item item = cache.getIfPresent(key);
        if (item != null) {
            @SuppressWarnings("unchecked")
            T hit = (T) item.value();
            return hit;
        }
        T value = loader.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evictAll() {
        cache.invalidateAll();
    }
}
