package com.owl.infra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内实现，只在 Redis 连不上时兜底。
 *
 * 局限：多实例不共享、重启丢数据。所以只用于开发机，日志里会明确警告。
 */
@Slf4j
public class MemoryKVStore implements KVStore {

    private record Entry(String value, long expireAtMillis) {
        boolean expired(long now) {
            return expireAtMillis > 0 && expireAtMillis <= now;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired(System.currentTimeMillis())) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, expireAt(ttl)));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        long now = System.currentTimeMillis();
        Entry fresh = new Entry(value, expireAt(ttl));
        Entry old = store.compute(key, (k, current) ->
                (current == null || current.expired(now)) ? fresh : current);
        return old == fresh;
    }

    @Override
    public long increment(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        AtomicLong result = new AtomicLong();
        store.compute(key, (k, current) -> {
            if (current == null || current.expired(now)) {
                result.set(1L);
                return new Entry("1", expireAt(ttl));
            }
            long next = Long.parseLong(current.value()) + 1;
            result.set(next);
            return new Entry(String.valueOf(next), current.expireAtMillis());
        });
        return result.get();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        store.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public boolean isRedis() {
        return false;
    }

    private long expireAt(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return 0L;
        }
        return System.currentTimeMillis() + ttl.toMillis();
    }

    /** 每 5 分钟扫一次过期键，防长期运行内存涨上去 */
    @Scheduled(fixedDelay = 300_000L)
    public void purge() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expired(now));
    }
}
