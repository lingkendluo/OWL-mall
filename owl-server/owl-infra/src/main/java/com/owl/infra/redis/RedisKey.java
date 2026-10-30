package com.owl.infra.redis;

/**
 * Redis key 规范：owl:{模块}:{业务}:{标识}。禁止 keys *、禁止无 TTL 的 key。
 */
public final class RedisKey {

    private static final String PREFIX = "owl:";

    private RedisKey() {
    }

    public static String smsCode(String scene, String phone) {
        return PREFIX + "sms:code:" + scene + ":" + phone;
    }

    public static String smsIp(String ip) {
        return PREFIX + "sms:ip:" + ip;
    }

    public static String smsResend(String scene, String phone) {
        return PREFIX + "sms:resend:" + scene + ":" + phone;
    }

    public static String smsAttempt(String scene, String phone) {
        return PREFIX + "sms:attempt:" + scene + ":" + phone;
    }

    public static String authRefresh(Long userId, String jti) {
        return PREFIX + "auth:refresh:" + userId + ":" + jti;
    }

    public static String authRefreshPattern(Long userId) {
        return PREFIX + "auth:refresh:" + userId + ":*";
    }

    public static String authFail(String account) {
        return PREFIX + "auth:fail:" + account;
    }

    public static String authBlack(String jti) {
        return PREFIX + "auth:black:" + jti;
    }

    public static String tradeToken(Long userId, String clientToken) {
        return PREFIX + "trade:token:" + userId + ":" + clientToken;
    }

    public static String orderSeq(String yyyyMMdd) {
        return PREFIX + "order:seq:" + yyyyMMdd;
    }

    public static String aiLimitUser(Long userId) {
        return PREFIX + "ai:limit:u:" + userId;
    }

    public static String aiLimitIp(String ip) {
        return PREFIX + "ai:limit:ip:" + ip;
    }

    public static String payMock(String payNo) {
        return PREFIX + "pay:mock:" + payNo;
    }

    public static String payLock(String payNo) {
        return "lock:pay:" + payNo;
    }

    public static String jobLock(String name) {
        return "lock:job:" + name;
    }

    public static String productCodeLock(String categoryCode) {
        return "lock:product:code:" + categoryCode;
    }

    public static String rateLimit(String userId, String path) {
        return PREFIX + "rate:" + userId + ":" + path;
    }
}
