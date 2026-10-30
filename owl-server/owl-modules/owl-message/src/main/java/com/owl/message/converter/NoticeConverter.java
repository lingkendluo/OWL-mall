package com.owl.message.converter;

import com.owl.common.util.TimeTextUtil;
import com.owl.message.entity.NNotice;
import com.owl.message.vo.NoticeVO;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class NoticeConverter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private NoticeConverter() {
    }

    public static NoticeVO toVO(NNotice notice) {
        NoticeVO vo = new NoticeVO();
        vo.setId(String.valueOf(notice.getId()));
        vo.setTitle(notice.getTitle());
        vo.setBody(notice.getBody());
        vo.setType(notice.getType());
        vo.setTypeText(typeText(notice.getType()));
        vo.setPinned(notice.getPinned() != null && notice.getPinned() == 1);
        vo.setStatus(notice.getStatus());
        vo.setStatusText(statusText(notice.getStatus()));
        LocalDateTime published = notice.getPublishAt() == null ? notice.getCreatedAt() : notice.getPublishAt();
        vo.setPublishAt(millis(notice.getPublishAt()));
        vo.setCreatedAt(millis(notice.getCreatedAt()));
        vo.setUpdatedAt(millis(notice.getUpdatedAt()));
        // 未发布的公告没有「发布时间」，文案退回创建时间，避免前端显示空
        vo.setTime(TimeTextUtil.format(published));
        return vo;
    }

    public static String typeText(Integer type) {
        if (type == null) {
            return "公告";
        }
        return switch (type) {
            case 2 -> "活动";
            case 3 -> "维护";
            case 4 -> "上新";
            default -> "公告";
        };
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "草稿";
        }
        return switch (status) {
            case 1 -> "已发布";
            case 2 -> "已下线";
            default -> "草稿";
        };
    }

    private static Long millis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
