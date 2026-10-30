package com.owl.message.service;

import com.owl.message.vo.NoticeVO;

import java.util.List;

/**
 * 用户端公告读取：只认「已发布」，草稿和已下线一律看不见。
 */
public interface NoticeService {

    /**
     * 首页横幅 / 公告页用。置顶优先，其次按发布时间倒序。
     *
     * @param limit 最多返回几条，<=0 时按 20 处理
     */
    List<NoticeVO> listPublished(int limit);

    NoticeVO detail(String id);
}
