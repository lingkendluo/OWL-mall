package com.owl.message.converter;

import com.owl.common.util.TimeTextUtil;
import com.owl.message.entity.NMessage;
import com.owl.message.vo.MessageVO;

import java.time.ZoneId;

public final class MessageConverter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private MessageConverter() {
    }

    public static MessageVO toVO(NMessage message) {
        MessageVO vo = new MessageVO();
        vo.setId(String.valueOf(message.getId()));
        vo.setKind(message.getKind());
        vo.setTitle(message.getTitle());
        vo.setBody(message.getBody());
        vo.setTime(TimeTextUtil.format(message.getCreatedAt()));
        vo.setRead(message.getReadFlag() != null && message.getReadFlag() == 1);
        vo.setCreatedAt(message.getCreatedAt() == null ? null
                : message.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli());
        vo.setBizNo(message.getBizNo());
        return vo;
    }
}
