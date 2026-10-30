package com.owl.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.IdUtils;
import com.owl.message.converter.NoticeConverter;
import com.owl.message.entity.NNotice;
import com.owl.message.mapper.NNoticeMapper;
import com.owl.message.service.NoticeService;
import com.owl.message.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NNoticeMapper noticeMapper;

    @Override
    public List<NoticeVO> listPublished(int limit) {
        int cap = limit <= 0 ? 20 : Math.min(limit, 50);
        List<NNotice> rows = noticeMapper.selectList(new LambdaQueryWrapper<NNotice>()
                .eq(NNotice::getStatus, 1)
                .orderByDesc(NNotice::getPinned)
                .orderByDesc(NNotice::getPublishAt)
                .orderByDesc(NNotice::getId)
                .last("LIMIT " + cap));
        return rows.stream().map(NoticeConverter::toVO).toList();
    }

    @Override
    public NoticeVO detail(String id) {
        NNotice notice = noticeMapper.selectOne(new LambdaQueryWrapper<NNotice>()
                .eq(NNotice::getId, IdUtils.toId(id))
                .eq(NNotice::getStatus, 1));
        if (notice == null) {
            throw new BizException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return NoticeConverter.toVO(notice);
    }
}
