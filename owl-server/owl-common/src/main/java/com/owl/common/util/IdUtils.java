package com.owl.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 轻量 ID 工具，不引雪花算法依赖。
 */
public final class IdUtils {

    private IdUtils() {
    }

    public static String uuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /** 定长数字串，用于会员号后缀、验证码 */
    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 对外 id 全是字符串，解析失败统一返回 0：查不到就由调用方抛「不存在」，
     * 比在每个 service 里 try/catch 干净。
     */
    public static long toId(String id) {
        if (id == null || id.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
