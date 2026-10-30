package com.owl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owl.auth.entity.MPointsLog;
import com.owl.auth.entity.MUser;
import com.owl.auth.mapper.MPointsLogMapper;
import com.owl.auth.mapper.MUserMapper;
import com.owl.auth.service.PointsService;
import com.owl.auth.vo.PointsLogVO;
import com.owl.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final MPointsLogMapper pointsLogMapper;
    private final MUserMapper userMapper;

    @Override
    public PageResult<PointsLogVO> list(Long userId, int page, int size) {
        Page<MPointsLog> result = pointsLogMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MPointsLog>()
                        .eq(MPointsLog::getUserId, userId)
                        .orderByDesc(MPointsLog::getCreatedAt)
                        .orderByDesc(MPointsLog::getId));
        return PageResult.of(result.getRecords().stream().map(log -> {
            PointsLogVO vo = new PointsLogVO();
            vo.setId(String.valueOf(log.getId()));
            vo.setChange(log.getChangeVal());
            vo.setType(log.getType());
            vo.setRemark(log.getRemark());
            vo.setCreatedAt(log.getCreatedAt() == null ? null
                    : log.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli());
            return vo;
        }).toList(), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, int delta, int type, String refNo, String remark) {
        if (delta == 0) {
            return;
        }
        MPointsLog log = new MPointsLog();
        log.setUserId(userId);
        log.setChangeVal(delta);
        log.setType(type);
        log.setRefNo(refNo);
        log.setRemark(remark);
        pointsLogMapper.insert(log);

        MUser user = userMapper.selectById(userId);
        if (user != null) {
            int next = Math.max(0, (user.getPoints() == null ? 0 : user.getPoints()) + delta);
            userMapper.update(null, new LambdaUpdateWrapper<MUser>()
                    .eq(MUser::getId, userId)
                    .set(MUser::getPoints, next));
        }
    }
}
