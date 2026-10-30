package com.owl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.admin.dto.AdminLoginRequest;
import com.owl.admin.entity.SAdmin;
import com.owl.admin.mapper.SAdminMapper;
import com.owl.admin.service.AdminAuthService;
import com.owl.admin.vo.AdminLoginVO;
import com.owl.admin.vo.AdminVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.context.AdminContext;
import com.owl.common.context.AdminUser;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import com.owl.infra.security.JwtService;
import com.owl.infra.security.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final int MAX_LOGIN_FAIL = 5;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(15);

    private final SAdminMapper adminMapper;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final KVStore kv;

    @Override
    public AdminLoginVO login(AdminLoginRequest request) {
        String username = request.getUsername().trim();
        // 与 C 端分开计数：同一个账号名不会互相影响
        String failKey = RedisKey.authFail("admin:" + username);
        String current = kv.get(failKey);
        if (current != null && Integer.parseInt(current) >= MAX_LOGIN_FAIL) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        SAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SAdmin>()
                .eq(SAdmin::getUsername, username));
        if (admin == null || !passwordService.matches(request.getPassword(), admin.getPasswordHash())) {
            kv.increment(failKey, LOGIN_LOCK);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        kv.delete(failKey);

        adminMapper.update(null, new LambdaUpdateWrapper<SAdmin>()
                .eq(SAdmin::getId, admin.getId())
                .set(SAdmin::getLastLoginAt, LocalDateTime.now()));

        String role = admin.getRole() == null ? "ops" : admin.getRole();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", admin.getUsername());
        claims.put("role", role);
        JwtService.IssuedToken token = jwtService.issueAccess(
                admin.getId(), JwtService.SCOPE_ADMIN, false, claims);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setAccessToken(token.token());
        vo.setExpiresIn(token.expiresIn());
        vo.setAdmin(toVO(admin, role));
        return vo;
    }

    @Override
    public AdminVO me() {
        AdminUser current = AdminContext.get();
        if (current == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        SAdmin admin = adminMapper.selectById(current.adminId());
        if (admin == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toVO(admin, current.role());
    }

    private AdminVO toVO(SAdmin admin, String role) {
        AdminVO vo = new AdminVO();
        vo.setId(String.valueOf(admin.getId()));
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setRole(role);
        return vo;
    }
}
