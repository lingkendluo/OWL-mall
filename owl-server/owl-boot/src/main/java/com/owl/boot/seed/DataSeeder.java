package com.owl.boot.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.admin.entity.SAdmin;
import com.owl.admin.mapper.SAdminMapper;
import com.owl.auth.entity.MUser;
import com.owl.auth.entity.MUserAddress;
import com.owl.auth.entity.MkUserCoupon;
import com.owl.auth.mapper.MUserAddressMapper;
import com.owl.auth.mapper.MUserMapper;
import com.owl.auth.mapper.MkUserCouponMapper;
import com.owl.auth.service.CouponService;
import com.owl.infra.config.SeedProperties;
import com.owl.infra.security.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示数据：管理员 + 测试用户 + 两张券 + 默认地址。
 * 全部按唯一键判重，已存在就跳过，不覆盖线上改过的密码和资料。
 * 生产把 owl.seed.enabled 置 false。
 */
@Slf4j
@Order(20)
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123456";

    private static final String DEMO_PHONE = "13800138000";
    private static final String DEMO_PASSWORD = "abc12345";

    private final SeedProperties seedProperties;
    private final SAdminMapper adminMapper;
    private final MUserMapper userMapper;
    private final MUserAddressMapper addressMapper;
    private final MkUserCouponMapper userCouponMapper;
    private final CouponService couponService;
    private final PasswordService passwordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!seedProperties.isEnabled()) {
            return;
        }
        seedAdmin();
        seedDemoUser();
    }

    private void seedAdmin() {
        Long exists = adminMapper.selectCount(new LambdaQueryWrapper<SAdmin>()
                .eq(SAdmin::getUsername, ADMIN_USERNAME));
        if (exists != null && exists > 0) {
            return;
        }
        SAdmin admin = new SAdmin();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPasswordHash(passwordService.encode(ADMIN_PASSWORD));
        admin.setNickname("超级管理员");
        admin.setRole("super");
        admin.setStatus(1);
        adminMapper.insert(admin);
        // 日志里不打印密码
        log.info("已初始化管理员账号 {}", ADMIN_USERNAME);
    }

    private void seedDemoUser() {
        MUser user = userMapper.selectOne(new LambdaQueryWrapper<MUser>()
                .eq(MUser::getPhone, DEMO_PHONE));
        if (user == null) {
            user = new MUser();
            user.setPhone(DEMO_PHONE);
            user.setPasswordHash(passwordService.encode(DEMO_PASSWORD));
            user.setNickname("罗耀");
            user.setLevel("black");
            user.setMemberNo("OWL-2026-0001");
            user.setPoints(2860);
            user.setBalanceCent(12050L);
            user.setIsGuest(0);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("已初始化测试用户 {}", DEMO_PHONE);
        }

        Long coupons = userCouponMapper.selectCount(new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getUserId, user.getId()));
        if (coupons == null || coupons == 0) {
            couponService.grant(user.getId(), "NEW10");
            couponService.grant(user.getId(), "FULL30");
        }

        Long addresses = addressMapper.selectCount(new LambdaQueryWrapper<MUserAddress>()
                .eq(MUserAddress::getUserId, user.getId()));
        if (addresses == null || addresses == 0) {
            addressMapper.insert(defaultAddress(user.getId()));
        }
    }

    private MUserAddress defaultAddress(Long userId) {
        MUserAddress address = new MUserAddress();
        address.setUserId(userId);
        address.setReceiver("罗耀");
        address.setPhone(DEMO_PHONE);
        address.setProvince("浙江省");
        address.setCity("杭州市");
        address.setDistrict("西湖区");
        address.setDetail("文三路 88 号 OWL 公寓 3 幢 1201 室");
        address.setTag("家");
        address.setIsDefault(1);
        return address;
    }
}
