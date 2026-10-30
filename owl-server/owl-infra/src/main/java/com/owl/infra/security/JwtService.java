package com.owl.infra.security;

import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.infra.config.JwtProperties;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 * JJWT 0.12.5 的 API 与网上 0.9 的示例不同：用 subject() / signWith(key) / verifyWith(key)。
 */
@Service
public class JwtService {

    public static final String SCOPE_USER = "user";
    public static final String SCOPE_ADMIN = "admin";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final KVStore kv;
    private final SecretKey key;

    public JwtService(JwtProperties props, KVStore kv) {
        this.props = props;
        this.kv = kv;
        byte[] secret = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("owl.jwt.secret 至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public record IssuedToken(String token, String jti, long expiresIn) {
    }

    public IssuedToken issueAccess(Long subjectId, String scope, boolean guest) {
        return issueAccess(subjectId, scope, guest, Map.of());
    }

    /** B 端登录要把 username / role 写进 token，拦截器回填 AdminContext 时要用 */
    public IssuedToken issueAccess(Long subjectId, String scope, boolean guest, Map<String, Object> extraClaims) {
        return issue(subjectId, scope, TYPE_ACCESS, props.getAccessTtl(), guest, extraClaims);
    }

    /** refresh token 必须落 Redis，否则无法吊销 */
    public IssuedToken issueRefresh(Long subjectId, String scope) {
        IssuedToken token = issue(subjectId, scope, TYPE_REFRESH, props.getRefreshTtl(), false, Map.of());
        kv.set(RedisKey.authRefresh(subjectId, token.jti()), "1", Duration.ofSeconds(props.getRefreshTtl()));
        return token;
    }

    private IssuedToken issue(Long subjectId, String scope, String type, long ttlSeconds, boolean guest,
                              Map<String, Object> extraClaims) {
        String jti = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claim("typ", type)
                .claim("scope", scope)
                .claim("guest", guest)
                .id(jti)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSeconds * 1000));
        extraClaims.forEach(builder::claim);
        String token = builder.signWith(key).compact();
        return new IssuedToken(token, jti, ttlSeconds);
    }

    /** 解析失败分过期 / 签名错 / 格式错三种，但对前端一律只说"登录已过期"，不暴露细节 */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期");
        } catch (Exception e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public Long subjectId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public String claim(Claims claims, String name) {
        Object value = claims.get(name);
        return value == null ? "" : String.valueOf(value);
    }

    public boolean claimBoolean(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isRevoked(String jti) {
        return jti != null && kv.exists(RedisKey.authBlack(jti));
    }

    /** 退出登录：把当前 access token 剩余有效期写进黑名单 */
    public void revokeAccess(String jti, Date expiration) {
        if (jti == null || expiration == null) {
            return;
        }
        long remain = expiration.getTime() - System.currentTimeMillis();
        if (remain > 0) {
            kv.set(RedisKey.authBlack(jti), "1", Duration.ofMillis(remain));
        }
    }

    public void revokeAllRefresh(Long userId) {
        kv.deleteByPattern(RedisKey.authRefreshPattern(userId));
    }

    public boolean refreshExists(Long userId, String jti) {
        return kv.exists(RedisKey.authRefresh(userId, jti));
    }

    public void revokeRefresh(Long userId, String jti) {
        kv.delete(RedisKey.authRefresh(userId, jti));
    }

    public long accessTtl() {
        return props.getAccessTtl();
    }
}
