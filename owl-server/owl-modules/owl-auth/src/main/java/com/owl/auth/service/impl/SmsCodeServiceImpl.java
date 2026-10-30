package com.owl.auth.service.impl;

import com.owl.auth.service.SmsCodeService;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.IdUtils;
import com.owl.infra.config.SmsProperties;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 验证码收在 Redis（key 见 7.7），以后换阿里云短信只改这个类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final int EXPIRE_SECONDS = 300;
    private static final int MAX_ATTEMPT = 5;
    private static final int IP_LIMIT_PER_HOUR = 20;

    private final KVStore kv;
    private final SmsProperties props;

    @Override
    public SmsCodeResult send(String phone, String scene, String ip) {
        String useScene = (scene == null || scene.isBlank()) ? "register" : scene;
        if (!"register".equals(useScene)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的验证码场景");
        }

        // 手机号 60 秒一条
        String resendKey = RedisKey.smsResend(useScene, phone);
        if (!kv.setIfAbsent(resendKey, "1", Duration.ofSeconds(props.getResendSeconds()))) {
            throw new BizException(ErrorCode.TOO_FREQUENT, "请 " + props.getResendSeconds() + " 秒后再试");
        }

        // 同 IP 每小时 20 条
        if (ip != null && !ip.isBlank()) {
            long count = kv.increment(RedisKey.smsIp(ip), Duration.ofHours(1));
            if (count > IP_LIMIT_PER_HOUR) {
                throw new BizException(ErrorCode.TOO_FREQUENT, "请求过于频繁，请稍后再试");
            }
        }

        String code = IdUtils.randomDigits(6);
        kv.set(RedisKey.smsCode(useScene, phone), code, Duration.ofSeconds(EXPIRE_SECONDS));
        kv.delete(RedisKey.smsAttempt(useScene, phone));
        // 日志里不打完整手机号
        log.info("短信验证码已生成 phone={}", IdUtils.maskPhone(phone));

        return new SmsCodeResult(EXPIRE_SECONDS, props.isReturnCode() ? code : null);
    }

    @Override
    public void verify(String phone, String scene, String code) {
        String useScene = (scene == null || scene.isBlank()) ? "register" : scene;
        String codeKey = RedisKey.smsCode(useScene, phone);
        String expected = kv.get(codeKey);
        if (expected == null) {
            throw new BizException(ErrorCode.SMS_CODE_ERROR);
        }
        if (!expected.equals(code)) {
            // 错 5 次作废，防暴力猜码
            long attempts = kv.increment(RedisKey.smsAttempt(useScene, phone), Duration.ofSeconds(EXPIRE_SECONDS));
            if (attempts >= MAX_ATTEMPT) {
                kv.delete(codeKey);
            }
            throw new BizException(ErrorCode.SMS_CODE_ERROR);
        }
        // 一次性使用
        kv.delete(codeKey);
        kv.delete(RedisKey.smsAttempt(useScene, phone));
    }
}
