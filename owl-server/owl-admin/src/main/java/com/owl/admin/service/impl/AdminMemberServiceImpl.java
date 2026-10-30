package com.owl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.admin.service.AdminMemberService;
import com.owl.admin.vo.AdminMemberVO;
import com.owl.auth.entity.MUser;
import com.owl.auth.mapper.MUserMapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.api.PageResult;
import com.owl.common.util.IdUtils;
import com.owl.common.util.TimeTextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMemberServiceImpl implements AdminMemberService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final MUserMapper userMapper;

    @Override
    public PageResult<AdminMemberVO> list(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<MUser> wrapper = new LambdaQueryWrapper<MUser>()
                .eq(status != null, MUser::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(MUser::getNickname, keyword)
                        .or()
                        .like(MUser::getPhone, keyword)
                        .or()
                        .like(MUser::getMemberNo, keyword))
                .orderByDesc(MUser::getId);
        Page<MUser> result = userMapper.selectPage(Page.of(page, size), wrapper);
        List<AdminMemberVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberVO changeStatus(String id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态只能是 0 或 1");
        }
        MUser user = userMapper.selectById(IdUtils.toId(id));
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会员不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return toVO(user);
    }

    private AdminMemberVO toVO(MUser user) {
        AdminMemberVO vo = new AdminMemberVO();
        vo.setId(String.valueOf(user.getId()));
        vo.setNickname(user.getNickname());
        vo.setPhone(IdUtils.maskPhone(user.getPhone()));
        vo.setEmail(user.getEmail());
        vo.setLevel(user.getLevel());
        vo.setLevelText(levelText(user.getLevel()));
        vo.setMemberNo(user.getMemberNo());
        vo.setPoints(user.getPoints() == null ? 0 : user.getPoints());
        vo.setBalance(user.getBalanceCent() == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(user.getBalanceCent(), 2));
        vo.setStatus(user.getStatus());
        vo.setGuest(user.getIsGuest() != null && user.getIsGuest() == 1);
        vo.setCreatedAt(millis(user.getCreatedAt()));
        vo.setLastLoginAt(millis(user.getLastLoginAt()));
        vo.setTime(TimeTextUtil.format(user.getCreatedAt()));
        return vo;
    }

    private String levelText(String level) {
        if (level == null) {
            return "普通会员";
        }
        return switch (level) {
            case "black" -> "OWL 黑卡会员";
            case "guest" -> "访客";
            default -> "普通会员";
        };
    }

    private Long millis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
