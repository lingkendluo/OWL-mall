package com.owl.infra.redis;

import java.time.Duration;

/**
 * 短生命周期键值存储的抽象。
 *
 * 存在的理由：业务只关心"存一个带 TTL 的字符串"，本机没起 Redis 时不应该整个后端跑不起来。
 * 生产环境用 Redis 实现（多实例共享），开发机自动降级到进程内实现（单实例够用）。
 */
public interface KVStore {

    String get(String key);

    void set(String key, String value, Duration ttl);

    /** SETNX，返回 true 表示抢到了 */
    boolean setIfAbsent(String key, String value, Duration ttl);

    /** 自增并设置过期时间（只在首次创建时设），返回自增后的值 */
    long increment(String key, Duration ttl);

    void delete(String key);

    /** 按前缀扫描删除，只用于 refresh token 这种明确的小集合，禁止业务里滥用 */
    void deleteByPattern(String pattern);

    boolean exists(String key);

    boolean isRedis();
}
