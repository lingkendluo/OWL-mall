package com.owl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.owl.auth.converter.AddressConverter;
import com.owl.auth.dto.AddressRequest;
import com.owl.auth.entity.MUserAddress;
import com.owl.auth.mapper.MUserAddressMapper;
import com.owl.auth.service.AddressService;
import com.owl.auth.vo.AddressVO;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESS = 20;

    private final MUserAddressMapper addressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<MUserAddress>()
                        .eq(MUserAddress::getUserId, userId)
                        .orderByDesc(MUserAddress::getIsDefault)
                        .orderByDesc(MUserAddress::getUpdatedAt))
                .stream().map(AddressConverter::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressRequest request) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<MUserAddress>()
                .eq(MUserAddress::getUserId, userId));
        if (count != null && count >= MAX_ADDRESS) {
            throw new BizException(ErrorCode.PARAM_ERROR, "最多保存 " + MAX_ADDRESS + " 个地址");
        }
        MUserAddress address = new MUserAddress();
        apply(address, request);
        address.setUserId(userId);
        boolean first = count == null || count == 0;
        address.setIsDefault(first || Boolean.TRUE.equals(request.getIsDefault()) ? 1 : 0);
        addressMapper.insert(address);
        if (address.getIsDefault() == 1) {
            clearOtherDefault(userId, address.getId());
        }
        return AddressConverter.toVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long id, AddressRequest request) {
        MUserAddress address = requireOwned(userId, id);
        apply(address, request);
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ? 1 : 0);
        addressMapper.updateById(address);
        if (address.getIsDefault() == 1) {
            clearOtherDefault(userId, id);
        }
        return AddressConverter.toVO(address);
    }

    @Override
    public void delete(Long userId, Long id) {
        MUserAddress address = requireOwned(userId, id);
        addressMapper.deleteById(address.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO setDefault(Long userId, Long id) {
        MUserAddress address = requireOwned(userId, id);
        addressMapper.update(null, new LambdaUpdateWrapper<MUserAddress>()
                .eq(MUserAddress::getUserId, userId)
                .ne(MUserAddress::getId, id)
                .set(MUserAddress::getIsDefault, 0));
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return AddressConverter.toVO(address);
    }

    @Override
    public AddressVO getOwned(Long userId, Long id) {
        return AddressConverter.toVO(requireOwned(userId, id));
    }

    @Override
    public AddressVO getOwnedOrDefault(Long userId, Long addressId) {
        if (addressId != null) {
            return AddressConverter.toVO(requireOwned(userId, addressId));
        }
        List<MUserAddress> list = addressMapper.selectList(new LambdaQueryWrapper<MUserAddress>()
                .eq(MUserAddress::getUserId, userId)
                .orderByDesc(MUserAddress::getIsDefault)
                .orderByDesc(MUserAddress::getUpdatedAt)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : AddressConverter.toVO(list.get(0));
    }

    /** 越权一律 10003，SQL 里就带 user_id，不要先查再在 Java 里比 */
    private MUserAddress requireOwned(Long userId, Long id) {
        MUserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<MUserAddress>()
                .eq(MUserAddress::getId, id)
                .eq(MUserAddress::getUserId, userId));
        if (address == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该地址");
        }
        return address;
    }

    private void clearOtherDefault(Long userId, Long keepId) {
        addressMapper.update(null, new LambdaUpdateWrapper<MUserAddress>()
                .eq(MUserAddress::getUserId, userId)
                .ne(MUserAddress::getId, keepId)
                .eq(MUserAddress::getIsDefault, 1)
                .set(MUserAddress::getIsDefault, 0));
    }

    private void apply(MUserAddress address, AddressRequest request) {
        address.setReceiver(request.getReceiver());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetail(request.getDetail());
        address.setTag(request.getTag());
    }
}
