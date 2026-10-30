package com.owl.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.owl.common.api.BizException;
import com.owl.common.api.ErrorCode;
import com.owl.common.util.IdUtils;
import com.owl.message.converter.NoticeConverter;
import com.owl.message.dto.NoticeUpsertRequest;
import com.owl.message.entity.NNotice;
import com.owl.message.mapper.NNoticeMapper;
import com.owl.message.service.NoticeAdminService;
import com.owl.message.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeAdminServiceImpl implements NoticeAdminService {

    private final NNoticeMapper noticeMapper;

    @Override
    public List<NoticeVO> list(String keyword, Integer status) {
        LambdaQueryWrapper<NNotice> query = new LambdaQueryWrapper<NNotice>()
                .eq(status != null, NNotice::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), NNotice::getTitle, keyword)
                .orderByDesc(NNotice::getPinned)
                .orderByDesc(NNotice::getId);
        return noticeMapper.selectList(query).stream().map(NoticeConverter::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVO create(NoticeUpsertRequest request, Long adminId) {
        NNotice notice = new NNotice();
        notice.setTitle(request.getTitle().trim());
        notice.setBody(request.getBody());
        notice.setType(normalizeType(request.getType()));
        notice.setPinned(Boolean.TRUE.equals(request.getPinned()) ? 1 : 0);
        notice.setAdminId(adminId);
        boolean publish = Boolean.TRUE.equals(request.getPublishNow());
        notice.setStatus(publish ? 1 : 0);
        notice.setPublishAt(publish ? LocalDateTime.now() : null);
        noticeMapper.insert(notice);
        return NoticeConverter.toVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVO update(String id, NoticeUpsertRequest request, Long adminId) {
        NNotice notice = require(id);
        notice.setTitle(request.getTitle().trim());
        notice.setBody(request.getBody());
        if (request.getType() != null) {
            notice.setType(normalizeType(request.getType()));
        }
        if (request.getPinned() != null) {
            notice.setPinned(request.getPinned() ? 1 : 0);
        }
        // 改一篇草稿并勾了"立即发布"，顺手补上发布时间
        if (Boolean.TRUE.equals(request.getPublishNow()) && (notice.getStatus() == null || notice.getStatus() != 1)) {
            notice.setStatus(1);
            notice.setPublishAt(LocalDateTime.now());
        }
        notice.setAdminId(adminId);
        noticeMapper.updateById(notice);
        return NoticeConverter.toVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVO publish(String id, Long adminId) {
        NNotice notice = require(id);
        notice.setStatus(1);
        // 重复点发布不刷新时间，保持第一次上架的时间线
        if (notice.getPublishAt() == null) {
            notice.setPublishAt(LocalDateTime.now());
        }
        notice.setAdminId(adminId);
        noticeMapper.updateById(notice);
        return NoticeConverter.toVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVO offline(String id, Long adminId) {
        NNotice notice = require(id);
        notice.setStatus(2);
        notice.setAdminId(adminId);
        noticeMapper.updateById(notice);
        return NoticeConverter.toVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        NNotice notice = require(id);
        noticeMapper.deleteById(notice.getId());
    }

    private NNotice require(String id) {
        NNotice notice = noticeMapper.selectById(IdUtils.toId(id));
        if (notice == null) {
            throw new BizException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private Integer normalizeType(Integer type) {
        if (type == null || type < 1 || type > 4) {
            return 1;
        }
        return type;
    }
}
