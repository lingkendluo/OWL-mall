package com.owl.auth.service;

import com.owl.auth.dto.AddressRequest;
import com.owl.auth.vo.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressRequest request);

    AddressVO update(Long userId, Long id, AddressRequest request);

    void delete(Long userId, Long id);

    AddressVO setDefault(Long userId, Long id);

    /** 越权访问抛 10003 */
    AddressVO getOwned(Long userId, Long id);

    /** 传 null 时回落到默认地址；没有地址返回 null */
    AddressVO getOwnedOrDefault(Long userId, Long addressId);
}
