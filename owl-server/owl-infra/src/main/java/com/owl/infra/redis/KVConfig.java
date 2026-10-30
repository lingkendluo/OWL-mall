package com.owl.infra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * KVStore 选择：启动时 ping 一次 Redis，通了用 Redis，没通用内存。
 * 这样"本机没装/没起 Redis"不会变成启动失败，而是打一行 warn 继续跑。
 */
@Slf4j
@Configuration
public class KVConfig {

    @Bean
    public KVStore kvStore(ObjectProvider<StringRedisTemplate> provider) {
        StringRedisTemplate template = provider.getIfAvailable();
        if (template != null) {
            try {
                template.execute((RedisCallback<String>) connection -> connection.ping());
                log.info("[owl] Redis 已连接，使用 RedisKVStore");
                return new RedisKVStore(template);
            } catch (Exception e) {
                log.warn("[owl] Redis 不可用（{}），降级为进程内 KVStore；多实例部署前必须修好 Redis", e.getMessage());
            }
        }
        return new MemoryKVStore();
    }
}
