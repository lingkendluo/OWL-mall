package com.owl.trade.service.impl;

import com.owl.auth.service.UserService;
import com.owl.auth.vo.UserVO;
import com.owl.trade.service.OrderService;
import com.owl.trade.service.ProfileService;
import com.owl.trade.vo.AssetsVO;
import com.owl.trade.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserService userService;
    private final OrderService orderService;

    @Override
    public ProfileVO profile(Long userId) {
        // UserVO 里已经带了积分、券数、余额，资产直接复用，不再查第二遍
        UserVO user = userService.getUserVO(userId);

        AssetsVO assets = new AssetsVO();
        assets.setCoupons(user.getCoupons());
        assets.setPoints(user.getPoints());
        assets.setBalance(user.getBalance());

        ProfileVO vo = new ProfileVO();
        vo.setUser(user);
        vo.setAssets(assets);
        vo.setOrderCounts(orderService.counts(userId));
        return vo;
    }
}
