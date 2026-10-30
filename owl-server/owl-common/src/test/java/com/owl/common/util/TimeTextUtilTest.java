package com.owl.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 站内信时间文案的 6 个分支。now 固定，避免测试跨零点随机失败。
 */
class TimeTextUtilTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 20, 0, 0);

    @Test
    @DisplayName("不到 1 分钟：刚刚")
    void justNow() {
        assertEquals("刚刚", TimeTextUtil.format(NOW.minusSeconds(30), NOW));
    }

    @Test
    @DisplayName("1 小时内：N 分钟前")
    void minutesAgo() {
        assertEquals("5 分钟前", TimeTextUtil.format(NOW.minusMinutes(5), NOW));
        assertEquals("59 分钟前", TimeTextUtil.format(NOW.minusMinutes(59), NOW));
    }

    @Test
    @DisplayName("当天超过 1 小时：N 小时前")
    void hoursAgo() {
        assertEquals("3 小时前", TimeTextUtil.format(NOW.minusHours(3), NOW));
    }

    @Test
    @DisplayName("昨天：昨天 HH:mm")
    void yesterday() {
        assertEquals("昨天 18:30", TimeTextUtil.format(LocalDateTime.of(2026, 3, 9, 18, 30), NOW));
    }

    @Test
    @DisplayName("今年更早：M 月 d 日")
    void sameYear() {
        assertEquals("1 月 8 日", TimeTextUtil.format(LocalDateTime.of(2026, 1, 8, 9, 0), NOW));
    }

    @Test
    @DisplayName("往年：yyyy 年 M 月 d 日")
    void otherYear() {
        assertEquals("2024 年 12 月 31 日", TimeTextUtil.format(LocalDateTime.of(2024, 12, 31, 23, 59), NOW));
    }
}
