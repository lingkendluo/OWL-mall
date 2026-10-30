package com.owl.auth.converter;

import com.owl.auth.entity.MUserAddress;
import com.owl.auth.vo.AddressVO;
import com.owl.common.util.IdUtils;

public final class AddressConverter {

    private AddressConverter() {
    }

    public static AddressVO toVO(MUserAddress address) {
        return toVO(address, false);
    }

    /**
     * 本人维护地址时传 mask=false（编辑要回填真实手机号，打码会存回脏数据）；
     * 订单、消息展示时传 mask=true。
     */
    public static AddressVO toVO(MUserAddress address, boolean mask) {
        AddressVO vo = new AddressVO();
        vo.setId(String.valueOf(address.getId()));
        vo.setReceiver(address.getReceiver());
        vo.setPhone(mask ? IdUtils.maskPhone(address.getPhone()) : address.getPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setTag(address.getTag());
        vo.setIsDefault(address.getIsDefault() != null && address.getIsDefault() == 1);
        return vo;
    }
}
