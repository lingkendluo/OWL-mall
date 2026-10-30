package com.owl.trade.service;

import com.owl.trade.vo.ProfileVO;

public interface ProfileService {

    /** 个人中心：用户 + 资产 + 订单角标，一次查完 */
    ProfileVO profile(Long userId);
}
