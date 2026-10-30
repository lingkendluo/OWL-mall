package com.owl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.auth.converter.UserConverter;
import com.owl.auth.entity.MUser;
import com.owl.auth.entity.MkUserCoupon;
import com.owl.auth.mapper.MUserMapper;
import com.owl.auth.mapper.MkUserCouponMapper;
import com.owl.auth.service.UserService;
import com.owl.auth.vo.UserVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final MUserMapper userMapper;
    private final MkUserCouponMapper userCouponMapper;

    @Override
    public MUser requireUser(Long userId) {
        MUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    public MUser getById(Long userId) {
        return userId == null ? null : userMapper.selectById(userId);
    }

    @Override
    public UserVO getUserVO(Long userId) {
        return toVO(requireUser(userId));
    }

    @Override
    public UserVO toVO(MUser user) {
        return UserConverter.toVO(user, countUsableCoupons(user.getId()));
    }

    @Override
    public int countUsableCoupons(Long userId) {
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<MkUserCoupon>()
                .eq(MkUserCoupon::getUserId, userId)
                .eq(MkUserCoupon::getStatus, 0));
        return count == null ? 0 : count.intValue();
    }
}
