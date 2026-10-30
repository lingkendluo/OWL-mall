package com.owl.infra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 实现。key 全部带 TTL，不存大对象。
 */
@Slf4j
public class RedisKVStore implements KVStore {

    private final StringRedisTemplate redis;

    public RedisKVStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redis.expire(key, ttl);
        }
        return value == null ? 0L : value;
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    @Override
    public boolean isRedis() {
        return true;
    }
}
