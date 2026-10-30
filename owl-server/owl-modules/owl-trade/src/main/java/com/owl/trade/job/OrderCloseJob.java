package com.owl.trade.job;

import com.owl.infra.config.TradeProperties;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import com.owl.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 超时未支付关单。多实例部署时用 Redis 锁保证只有一个节点在跑，
 * 锁 TTL 比执行间隔长一点，进程被 kill 也能自己解开。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseJob {

    private static final String LOCK_NAME = "order-close";

    private final OrderService orderService;
    private final TradeProperties tradeProperties;
    private final KVStore kv;

    @Scheduled(initialDelay = 60_000, fixedDelay = 120_000)
    public void closeExpired() {
        String key = RedisKey.jobLock(LOCK_NAME);
        if (!kv.setIfAbsent(key, "1", Duration.ofMinutes(5))) {
            return;
        }
        try {
            int count = orderService.closeExpiredOrders(tradeProperties.getOrderExpireMinutes());
            if (count > 0) {
                log.info("关闭超时未支付订单 {} 笔", count);
            }
        } catch (Exception e) {
            log.error("关闭超时订单失败", e);
        } finally {
            kv.delete(key);
        }
    }
}
