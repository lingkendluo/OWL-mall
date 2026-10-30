package com.owl.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 站内信时间的展示文案。前端直接用，不再自己算相对时间。
 * 规则见规格书 6.8.1 的表格。
 */
public final class TimeTextUtil {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter M_D = DateTimeFormatter.ofPattern("M 月 d 日");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日");

    private TimeTextUtil() {
    }

    public static String format(LocalDateTime time) {
        return format(time, LocalDateTime.now());
    }

    public static String format(LocalDateTime time, LocalDateTime now) {
        if (time == null) {
            return "";
        }
        long minutes = java.time.Duration.between(time, now).toMinutes();
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        LocalDate today = now.toLocalDate();
        if (time.toLocalDate().equals(today)) {
            return java.time.Duration.between(time, now).toHours() + " 小时前";
        }
        if (time.toLocalDate().equals(today.minusDays(1))) {
            return "昨天 " + time.format(HH_MM);
        }
        if (time.getYear() == now.getYear()) {
            return time.format(M_D);
        }
        return time.format(YMD);
    }
}
