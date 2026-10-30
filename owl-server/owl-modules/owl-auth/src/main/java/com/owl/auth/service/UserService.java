package com.owl.auth.service;

import com.owl.auth.entity.MUser;
import com.owl.auth.vo.UserVO;

public interface UserService {

    MUser requireUser(Long userId);

    MUser getById(Long userId);

    UserVO getUserVO(Long userId);

    UserVO toVO(MUser user);

    /** 未使用券张数，个人中心与 UserVO.coupons 共用 */
    int countUsableCoupons(Long userId);
}
