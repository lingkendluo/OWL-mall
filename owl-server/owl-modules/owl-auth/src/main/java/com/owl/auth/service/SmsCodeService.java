package com.owl.auth.service;

public interface SmsCodeService {

    SmsCodeResult send(String phone, String scene, String ip);

    void verify(String phone, String scene, String code);

    /**
     * @param expireIn 有效期秒数
     * @param code     dev 环境回传方便联调，prod 为 null
     */
    record SmsCodeResult(int expireIn, String code) {
    }
}
