package com.owl.infra.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 只引 spring-security-crypto 的 BCrypt，不引 Spring Security 全家桶。
 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public String encode(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String hash) {
        if (raw == null || hash == null) {
            return false;
        }
        return encoder.matches(raw, hash);
    }
}
