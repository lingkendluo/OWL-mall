package com.owl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.auth.dto.ChangePasswordRequest;
import com.owl.auth.dto.LoginRequest;
import com.owl.auth.dto.RegisterRequest;
import com.owl.auth.dto.UpdateProfileRequest;
import com.owl.auth.entity.MUser;
import com.owl.auth.mapper.MUserMapper;
import com.owl.auth.service.AuthService;
import com.owl.auth.service.CouponService;
import com.owl.auth.service.SmsCodeService;
import com.owl.auth.service.UserService;
import com.owl.auth.vo.LoginVO;
import com.owl.auth.vo.TokenVO;
import com.owl.auth.vo.UserVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.context.LoginUser;
import com.owl.common.context.UserContext;
import com.owl.common.util.IdUtils;
import com.owl.infra.redis.KVStore;
import com.owl.infra.redis.RedisKey;
import com.owl.infra.security.JwtService;
import com.owl.infra.security.PasswordService;
import com.owl.message.enums.MessageKind;
import com.owl.message.enums.MessageTemplate;
import com.owl.message.service.MessageService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_FAIL = 5;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(15);

    private final MUserMapper userMapper;
    private final UserService userService;
    private final CouponService couponService;
    private final MessageService messageService;
    private final SmsCodeService smsCodeService;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final KVStore kv;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getAgree())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请先同意用户协议");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<MUser>()
                .eq(MUser::getPhone, request.getPhone()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PHONE_REGISTERED);
        }
        smsCodeService.verify(request.getPhone(), "register", request.getCode());

        MUser user = new MUser();
        user.setPhone(request.getPhone());
        user.setNickname(request.getName());
        user.setPasswordHash(passwordService.encode(request.getPassword()));
        user.setLevel("normal");
        user.setMemberNo(nextMemberNo());
        user.setPoints(0);
        user.setBalanceCent(0L);
        user.setIsGuest(0);
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);

        // 送券 + 欢迎消息必须和建用户在同一个事务里，任何一步失败整体回滚
        couponService.grantNewUser(user.getId());
        messageService.sendTemplate(user.getId(), MessageKind.SYSTEM, MessageTemplate.WELCOME, null, user.getNickname());

        return buildLogin(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        String account = request.getAccount().trim();
        String failKey = RedisKey.authFail(account);
        String current = kv.get(failKey);
        if (current != null && Integer.parseInt(current) >= MAX_LOGIN_FAIL) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        MUser user = account.contains("@")
                ? userMapper.selectOne(new LambdaQueryWrapper<MUser>().eq(MUser::getEmail, account))
                : userMapper.selectOne(new LambdaQueryWrapper<MUser>().eq(MUser::getPhone, account));

        // 账号不存在与密码错误返回同一个码，防账号枚举
        if (user == null || !passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            kv.increment(failKey, LOGIN_LOCK);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        kv.delete(failKey);
        userMapper.update(null, new LambdaUpdateWrapper<MUser>()
                .eq(MUser::getId, user.getId())
                .set(MUser::getLastLoginAt, LocalDateTime.now()));
        return buildLogin(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO guest() {
        MUser user = new MUser();
        user.setNickname("访客");
        user.setPasswordHash(passwordService.encode(UUID.randomUUID().toString()));
        user.setLevel("guest");
        user.setPoints(0);
        user.setBalanceCent(0L);
        user.setIsGuest(1);
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);
        return buildLogin(user);
    }

    @Override
    public TokenVO refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!JwtService.TYPE_REFRESH.equals(jwtService.claim(claims, "typ"))) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = jwtService.subjectId(claims);
        String jti = claims.getId();
        // refresh token 必须还在 Redis 里，退出登录 / 改密码会删掉
        if (!jwtService.refreshExists(userId, jti)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        MUser user = userService.requireUser(userId);
        String scope = jwtService.claim(claims, "scope");

        // 轮换：旧的立刻作废
        jwtService.revokeRefresh(userId, jti);
        boolean guest = user.getIsGuest() != null && user.getIsGuest() == 1;
        JwtService.IssuedToken access = jwtService.issueAccess(userId, scope, guest);
        JwtService.IssuedToken refresh = jwtService.issueRefresh(userId, scope);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(access.token());
        vo.setRefreshToken(refresh.token());
        vo.setExpiresIn(access.expiresIn());
        return vo;
    }

    @Override
    public void logout() {
        LoginUser current = UserContext.get();
        if (current == null) {
            return;
        }
        jwtService.revokeAccess(current.jti(), new java.util.Date(System.currentTimeMillis() + jwtService.accessTtl() * 1000));
        jwtService.revokeAllRefresh(current.userId());
    }

    @Override
    public UserVO me(Long userId) {
        return userService.getUserVO(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        MUser user = userService.requireUser(userId);
        LambdaUpdateWrapper<MUser> update = new LambdaUpdateWrapper<MUser>().eq(MUser::getId, userId);
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            update.set(MUser::getNickname, request.getNickname().trim());
            user.setNickname(request.getNickname().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim();
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<MUser>()
                    .eq(MUser::getEmail, email)
                    .ne(MUser::getId, userId));
            if (exists != null && exists > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "邮箱已被使用");
            }
            update.set(MUser::getEmail, email);
            user.setEmail(email);
        }
        if (request.getAvatar() != null) {
            update.set(MUser::getAvatar, request.getAvatar().trim());
            user.setAvatar(request.getAvatar().trim());
        }
        userMapper.update(null, update);
        return userService.toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        MUser user = userService.requireUser(userId);
        if (user.getIsGuest() != null && user.getIsGuest() == 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "游客账号请先注册");
        }
        if (!passwordService.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.LOGIN_FAILED, "原密码不正确");
        }
        userMapper.update(null, new LambdaUpdateWrapper<MUser>()
                .eq(MUser::getId, userId)
                .set(MUser::getPasswordHash, passwordService.encode(request.getNewPassword())));
        // 改密码后踢掉所有 refresh token，强制重新登录
        jwtService.revokeAllRefresh(userId);
    }

    private LoginVO buildLogin(MUser user) {
        boolean guest = user.getIsGuest() != null && user.getIsGuest() == 1;
        JwtService.IssuedToken access = jwtService.issueAccess(user.getId(), JwtService.SCOPE_USER, guest);
        JwtService.IssuedToken refresh = jwtService.issueRefresh(user.getId(), JwtService.SCOPE_USER);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(access.token());
        vo.setRefreshToken(refresh.token());
        vo.setExpiresIn(access.expiresIn());
        vo.setUser(userService.toVO(user));
        return vo;
    }

    /** OWL-2026-4821，撞唯一索引的概率极低，撞了就换一个 */
    private String nextMemberNo() {
        int year = Year.now().getValue();
        for (int i = 0; i < 5; i++) {
            String memberNo = "OWL-" + year + "-" + IdUtils.randomDigits(4);
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<MUser>()
                    .eq(MUser::getMemberNo, memberNo));
            if (exists == null || exists == 0) {
                return memberNo;
            }
        }
        return "OWL-" + year + "-" + IdUtils.randomDigits(8);
    }
}
