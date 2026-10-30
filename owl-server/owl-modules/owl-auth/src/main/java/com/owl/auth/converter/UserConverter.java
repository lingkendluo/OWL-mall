package com.owl.auth.converter;

import com.owl.auth.entity.MUser;
import com.owl.auth.vo.UserVO;
import com.owl.common.util.PriceUtil;

import java.time.ZoneId;

public final class UserConverter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private UserConverter() {
    }

    public static UserVO toVO(MUser user, int couponCount) {
        UserVO vo = new UserVO();
        // id 必须是 String：前端 normalize() 会因为 number 丢掉整个会话
        vo.setId(String.valueOf(user.getId()));
        vo.setName(user.getNickname());
        vo.setPhone(user.getPhone() == null ? "" : user.getPhone());
        vo.setEmail(user.getEmail() == null ? "" : user.getEmail());
        vo.setLevel(user.getLevel());
        vo.setMemberNo(user.getMemberNo() == null ? "" : user.getMemberNo());
        vo.setPoints(user.getPoints() == null ? 0 : user.getPoints());
        vo.setCoupons(couponCount);
        vo.setBalance(PriceUtil.toYuan(user.getBalanceCent() == null ? 0L : user.getBalanceCent()));
        vo.setJoinedAt(user.getCreatedAt() == null ? null : user.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli());
        vo.setIsGuest(user.getIsGuest() != null && user.getIsGuest() == 1);
        return vo;
    }
}
