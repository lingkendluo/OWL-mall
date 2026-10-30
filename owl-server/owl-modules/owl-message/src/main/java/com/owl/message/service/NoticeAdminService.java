package com.owl.message.service;

import com.owl.message.dto.NoticeUpsertRequest;
import com.owl.message.vo.NoticeVO;

import java.util.List;

/**
 * 管理端公告：草稿 → 发布 → 下线，随时可改。
 */
public interface NoticeAdminService {

    /**
     * @param keyword 标题模糊搜索，可空
     * @param status  0 草稿 1 已发布 2 已下线，null 表示全部
     */
    List<NoticeVO> list(String keyword, Integer status);

    NoticeVO create(NoticeUpsertRequest request, Long adminId);

    NoticeVO update(String id, NoticeUpsertRequest request, Long adminId);

    NoticeVO publish(String id, Long adminId);

    NoticeVO offline(String id, Long adminId);

    void delete(String id);
}
